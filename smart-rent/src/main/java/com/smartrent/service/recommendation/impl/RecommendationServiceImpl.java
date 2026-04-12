package com.smartrent.service.recommendation.impl;

import com.smartrent.dto.request.AIRecommendationRequest;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.RecommendationItemDto;
import com.smartrent.dto.response.RecommendationResponse;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.PhoneClickDetailRepository;
import com.smartrent.infra.repository.SavedListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.PhoneClickDetail;
import com.smartrent.infra.repository.entity.SavedListing;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.recommendation.RecommendationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    ListingRepository listingRepository;
    SavedListingRepository savedListingRepository;
    PhoneClickDetailRepository phoneClickDetailRepository;
    com.smartrent.service.recentlyviewed.RecentlyViewedService recentlyViewedService;
    SmartRentAiConnector aiConnector;
    ListingService listingService;

    @Override
    public RecommendationResponse getSimilarListings(Long listingId, int topN) {
        Listing target = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Target listing not found"));

        if (target.getAddress() == null) {
            return RecommendationResponse.builder().listings(new ArrayList<>()).mode("similar").totalReturned(0).build();
        }

        Integer provinceId = target.getAddress().getLegacyProvinceId();
        String provinceCode = target.getAddress().getNewProvinceCode();

        List<Listing> candidates;
        if (provinceId != null || provinceCode != null) {
            candidates = listingRepository.findCandidatesForSimilar(
                provinceId, provinceCode, target.getProductType(), target.getListingType(), listingId, PageRequest.of(0, 200));
        } else {
            log.debug("Province is NULL for listingId={}, using Global candidates", listingId);
            candidates = listingRepository.findCandidatesForSimilarGlobal(
                target.getProductType(), target.getListingType(), listingId, PageRequest.of(0, 200));
        }
        
        log.debug("Similar candidates count={} for listingId={}", candidates.size(), listingId);

        if (candidates.isEmpty()) {
            log.warn("No candidates found for listingId={}, trying global fallback", listingId);
            candidates = listingRepository.findAll().stream()
                .filter(l -> !l.getListingId().equals(listingId))
                .filter(l -> Boolean.TRUE.equals(l.getVerified()))
                .filter(l -> Boolean.FALSE.equals(l.getExpired()))
                .limit(10)
                .collect(Collectors.toList());
        }
        
        if (candidates.isEmpty()) {
            return RecommendationResponse.builder().listings(new ArrayList<>()).mode("similar").totalReturned(0).build();
        }

        AIRecommendationRequest.SimilarListingAiRequest aiRequest = AIRecommendationRequest.SimilarListingAiRequest.builder()
                .target(toListingFeatureDto(target))
                .candidates(candidates.stream().map(this::toListingFeatureDto).collect(Collectors.toList()))
                .top_n(topN)
                .alpha(0.0) // Ignore CF for similar items
                .build();

        try {
            List<RecommendationItemDto> recommendedItems = aiConnector.getSimilarListings(aiRequest);
            return buildResponse(recommendedItems, "similar", false);
        } catch (Exception e) {
            log.error("Error calling AI service for similar listings", e);
            // Fallback
            List<RecommendationItemDto> fallbacks = candidates.stream().limit(topN)
                    .map(c -> RecommendationItemDto.builder().listingId(c.getListingId()).score(0.0).build()).collect(Collectors.toList());
            return buildResponse(fallbacks, "similar_fallback", true);
        }
    }

    @Override
    public RecommendationResponse getPersonalizedFeed(String userId, int topN) {
        // Collect explicit user interactions
        List<AIRecommendationRequest.InteractionEntryDto> userInteractions = new ArrayList<>();
        Set<Long> interactedListingIds = new HashSet<>();

        // 1. Saved Listings (Weight 3.0)
        List<SavedListing> saved = savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (SavedListing s : saved) {
            userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, s.getId().getListingId(), 3.0));
            interactedListingIds.add(s.getId().getListingId());
        }

        // 2. Phone Clicks (Weight 2.5)
        List<Long> clickedListingIds = phoneClickDetailRepository.findListingIdsByUserId(userId);
        for (Long lId : clickedListingIds) {
            userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, lId, 2.5));
            interactedListingIds.add(lId);
        }

        // 3. User Browse History (Weight 1.0)
        List<com.smartrent.dto.response.RecentlyViewedItemResponse> viewedListings = recentlyViewedService.get(userId);
        for (com.smartrent.dto.response.RecentlyViewedItemResponse res : viewedListings) {
            Long lId = res.getListing().getListingId();
            if (!interactedListingIds.contains(lId)) { // Only add if not already captured by higher weight
                userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, lId, 1.0));
                interactedListingIds.add(lId);
            }
        }

        log.info("Personalized feed for user={}: {} interactions collected", userId, userInteractions.size());
        
        // --- COLD START LOGIC ---
        if (userInteractions.isEmpty()) {
            log.info("No interactions found for user={}, triggering cold start", userId);
            return getColdStartFeed(topN);
        }

        // Attempt to determine province bias
        List<Listing> interactedListings = listingRepository.findByListingIdIn(interactedListingIds);
        Integer preferredProvinceId = null;
        String preferredProvinceCode = null;

        if (!interactedListings.isEmpty()) {
            Listing mostRecent = interactedListings.get(0);
            if (mostRecent.getAddress() != null) {
                preferredProvinceId = mostRecent.getAddress().getLegacyProvinceId();
                preferredProvinceCode = mostRecent.getAddress().getNewProvinceCode();
            }
        }

        List<Listing> candidates = new ArrayList<>();
        if (preferredProvinceId != null || preferredProvinceCode != null) {
            candidates = listingRepository.findCandidatesForPersonalized(
                preferredProvinceId, preferredProvinceCode, new ArrayList<>(interactedListingIds), PageRequest.of(0, 300));
        } 
        
        if (candidates.isEmpty()) {
            log.info("No local Province candidates for user={}, using Global candidates", userId);
            candidates = listingRepository.findCandidatesForPersonalizedGlobal(
                new ArrayList<>(interactedListingIds), PageRequest.of(0, 300));
        }

        log.debug("Candidate pool size={} for user={}", candidates.size(), userId);

        if (candidates.isEmpty()) {
            log.info("Candidate pool empty after interaction filter for user={}, triggering cold start", userId);
            return getColdStartFeed(topN);
        }

        // Collect all interactions strictly on candidate listings to build CF item-item matrix in AI
        List<Long> candidateIds = candidates.stream().map(Listing::getListingId).collect(Collectors.toList());
        List<AIRecommendationRequest.InteractionEntryDto> allInteractions = new ArrayList<>();

        List<SavedListing> allSaved = savedListingRepository.findByIdListingIdIn(candidateIds);
        allSaved.forEach(s -> allInteractions.add(new AIRecommendationRequest.InteractionEntryDto(s.getId().getUserId(), s.getId().getListingId(), 3.0)));

        List<PhoneClickDetail> allClicks = phoneClickDetailRepository.findByListingIdIn(candidateIds);
        allClicks.forEach(c -> allInteractions.add(new AIRecommendationRequest.InteractionEntryDto(c.getUser().getUserId(), c.getListing().getListingId(), 2.5)));


        AIRecommendationRequest.PersonalizedFeedAiRequest aiRequest = AIRecommendationRequest.PersonalizedFeedAiRequest.builder()
                .user_id(userId)
                .user_interactions(userInteractions)
                .all_interactions(allInteractions)
                .candidates(candidates.stream().map(this::toListingFeatureDto).collect(Collectors.toList()))
                .top_n(topN)
                .alpha(0.5) // Balance CF & CBF
                .build();

        log.debug("Calling AI service for personalized feed, user={}", userId);
        List<RecommendationItemDto> recommendedItems = aiConnector.getPersonalizedFeed(aiRequest);
        return buildResponse(recommendedItems, "personalized", false);
    }

    private RecommendationResponse getColdStartFeed(int topN) {
        log.info("Triggering cold start recommendation feed");
        List<Listing> candidates = listingRepository.findCandidatesForPersonalizedGlobal(List.of(-1L), PageRequest.of(0, topN * 5));

        candidates.sort((c1, c2) -> {
            int rank1 = getVipRank(c1.getVipType());
            int rank2 = getVipRank(c2.getVipType());
            return Integer.compare(rank2, rank1); // Descending rank
        });

        List<RecommendationItemDto> items = candidates.stream()
                .limit(topN)
                .map(c -> RecommendationItemDto.builder().listingId(c.getListingId()).score(0.0).build())
                .collect(Collectors.toList());
                
        return buildResponse(items, "cold_start", true);
    }

    private int getVipRank(Listing.VipType vipType) {
        if (vipType == null) return 1;
        return switch (vipType.name()) {
            case "DIAMOND" -> 4;
            case "GOLD" -> 3;
            case "SILVER" -> 2;
            default -> 1;
        };
    }

    private RecommendationResponse buildResponse(List<RecommendationItemDto> recommendedItems, String mode, boolean isColdStart) {
        if (recommendedItems == null || recommendedItems.isEmpty()) {
            return RecommendationResponse.builder().listings(new ArrayList<>()).mode(mode).totalReturned(0).coldStart(isColdStart).build();
        }

        Set<Long> idsToFetch = recommendedItems.stream().map(RecommendationItemDto::getListingId).collect(Collectors.toSet());
        List<ListingResponse> fullListings = listingService.getListingsByIds(idsToFetch);

        Map<Long, ListingResponse> listingMap = fullListings.stream().collect(Collectors.toMap(ListingResponse::getListingId, l -> l));

        List<ListingResponse> sortedResponses = new ArrayList<>();
        for (RecommendationItemDto item : recommendedItems) {
            ListingResponse res = listingMap.get(item.getListingId());
            if (res != null) {
                sortedResponses.add(res);
            }
        }

        return RecommendationResponse.builder()
                .listings(sortedResponses)
                .mode(mode)
                .totalReturned(sortedResponses.size())
                .coldStart(isColdStart)
                .build();
    }

    private AIRecommendationRequest.ListingFeatureDto toListingFeatureDto(Listing listing) {
        int daysAgo = 0;
        LocalDateTime activeDate = listing.getPushedAt() != null ? listing.getPushedAt() : listing.getPostDate();
        if (activeDate != null) {
            daysAgo = (int) ChronoUnit.DAYS.between(activeDate, LocalDateTime.now());
            if (daysAgo < 0) daysAgo = 0; 
        }

        String pCode = null;
        Integer dId = null;
        if (listing.getAddress() != null) {
            pCode = listing.getAddress().getNewProvinceCode();
            if (pCode == null && listing.getAddress().getLegacyProvinceId() != null) {
                pCode = String.valueOf(listing.getAddress().getLegacyProvinceId());
            }
            dId = listing.getAddress().getLegacyDistrictId();
        }

        return AIRecommendationRequest.ListingFeatureDto.builder()
                .listingId(listing.getListingId())
                .productType(listing.getProductType() != null ? listing.getProductType().name() : "ROOM")
                .listingType(listing.getListingType() != null ? listing.getListingType().name() : "RENT")
                .price(listing.getPrice() != null ? listing.getPrice().doubleValue() : 0.0)
                .area(listing.getArea() != null ? listing.getArea() : 0.0)
                .bedrooms((listing.getBedrooms() != null && listing.getBedrooms() > 0) ? listing.getBedrooms() : 0)
                .provinceCode(pCode != null ? pCode : "UNKNOWN")
                .districtId(dId)
                .vipType(listing.getVipType() != null ? listing.getVipType().name() : "NORMAL")
                .postDateDaysAgo(daysAgo)
                .build();
    }
}
