package com.smartrent.service.recommendation.impl;

import com.smartrent.dto.request.AIRecommendationRequest;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.RecentlyViewedItemResponse;
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
import com.smartrent.service.recentlyviewed.RecentlyViewedService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    ListingRepository listingRepository;
    SavedListingRepository savedListingRepository;
    PhoneClickDetailRepository phoneClickDetailRepository;
    RecentlyViewedService recentlyViewedService;
    SmartRentAiConnector aiConnector;
    ListingService listingService;

    @Override
    public RecommendationResponse getSimilarListings(Long listingId, int topN, String userId) {
        Listing target = listingRepository.findByIdWithAddress(listingId).orElse(null);
        if (target == null) {
            return RecommendationResponse.builder().listings(new ArrayList<>()).mode("similar").totalReturned(0)
                    .build();
        }

        com.smartrent.infra.repository.entity.Address targetAddr = target.getAddress();
        Integer provinceId = (targetAddr != null) ? targetAddr.getLegacyProvinceId() : null;
        String provinceCode = (targetAddr != null) ? targetAddr.getNewProvinceCode() : null;

        List<Listing> candidates;
        if (provinceId != null || provinceCode != null) {
            candidates = listingRepository.findCandidatesForSimilar(
                    provinceId, provinceCode, target.getProductType(), target.getListingType(), listingId,
                    PageRequest.of(0, 200));
        } else {
            candidates = listingRepository.findCandidatesForSimilarGlobal(
                    target.getProductType(), target.getListingType(), listingId, PageRequest.of(0, 200));
        }

        if (candidates.isEmpty()) {
            return RecommendationResponse.builder().listings(new ArrayList<>()).mode("similar").totalReturned(0)
                    .build();
        }

        List<AIRecommendationRequest.InteractionEntryDto> userInteractions = null;
        Set<Long> seenIds = new HashSet<>();
        if (userId != null) {
            userInteractions = new ArrayList<>();
            for (SavedListing s : savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
                userInteractions
                        .add(new AIRecommendationRequest.InteractionEntryDto(userId, s.getId().getListingId(), 3.0));
                seenIds.add(s.getId().getListingId());
            }
            for (Long lId : phoneClickDetailRepository.findListingIdsByUserId(userId)) {
                userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, lId, 2.5));
                seenIds.add(lId);
            }
            for (RecentlyViewedItemResponse res : recentlyViewedService.get(userId)) {
                Long lId = res.getListing().getListingId();
                if (!seenIds.contains(lId)) {
                    userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, lId, 1.0));
                    seenIds.add(lId);
                }
            }
        }

        List<AIRecommendationRequest.ListingFeatureDto> interactionFeatures = null;
        if (userId != null && !seenIds.isEmpty()) {
            interactionFeatures = listingRepository.findByListingIdIn(seenIds).stream()
                    .map(this::toListingFeatureDto).collect(Collectors.toList());
        }

        AIRecommendationRequest.SimilarListingAiRequest aiRequest = AIRecommendationRequest.SimilarListingAiRequest
                .builder()
                .target(toListingFeatureDto(target))
                .candidates(candidates.stream().map(this::toListingFeatureDto).collect(Collectors.toList()))
                .top_n(topN)
                .alpha(0.6)
                .userInteractions(userInteractions)
                .interactionFeatures(interactionFeatures)
                .build();

        try {
            List<RecommendationItemDto> recommendedItems = aiConnector.getSimilarListings(aiRequest);
            return buildResponse(recommendedItems, (userId != null) ? "similar_personalized" : "similar", false, false);
        } catch (Exception e) {
            log.error("AI service error calling similar API", e);
            List<RecommendationItemDto> fallbacks = candidates.stream().limit(topN)
                    .map(c -> RecommendationItemDto.builder().listingId(c.getListingId()).score(0.0).build())
                    .collect(Collectors.toList());
            return buildResponse(fallbacks, "similar_fallback", true, false);
        }
    }

    @Override
    public RecommendationResponse getPersonalizedFeed(String userId, int topN) {
        // FIX #1: Deduplicate interactions — keep MAX weight per listing
        Map<Long, Double> interactionWeightMap = new LinkedHashMap<>();

        for (SavedListing s : savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            interactionWeightMap.merge(s.getId().getListingId(), 3.0, Math::max);
        }
        for (Long lId : phoneClickDetailRepository.findListingIdsByUserId(userId)) {
            interactionWeightMap.merge(lId, 2.5, Math::max);
        }
        for (RecentlyViewedItemResponse res : recentlyViewedService.get(userId)) {
            interactionWeightMap.merge(res.getListing().getListingId(), 1.0, Math::max);
        }

        if (interactionWeightMap.isEmpty()) {
            return getColdStartFeed(topN);
        }

        List<AIRecommendationRequest.InteractionEntryDto> userInteractions = new ArrayList<>();
        Set<Long> interactedListingIds = interactionWeightMap.keySet();
        for (Map.Entry<Long, Double> entry : interactionWeightMap.entrySet()) {
            userInteractions
                    .add(new AIRecommendationRequest.InteractionEntryDto(userId, entry.getKey(), entry.getValue()));
        }

        if (userInteractions.isEmpty()) {
            return getColdStartFeed(topN);
        }
        // 2. Fetch features of interacted items for profiling
        List<Listing> interactedListings = listingRepository.findByListingIdIn(interactedListingIds);
        List<AIRecommendationRequest.ListingFeatureDto> interactionFeatures = interactedListings.stream()
                .map(this::toListingFeatureDto).collect(Collectors.toList());

        // 3. Location Profiling: Pick most frequent province
        String preferredProvinceCode = null;
        Map<String, Long> provinceCodeCounts = interactionFeatures.stream()
                .filter(f -> f.getProvinceCode() != null && !f.getProvinceCode().equals("UNKNOWN"))
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getProvinceCode, Collectors.counting()));

        preferredProvinceCode = provinceCodeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 4. Hybrid Candidate Pool (Local-First Discovery Channel)
        List<Listing> candidates = new ArrayList<>();
        Set<Long> excludedIds = new HashSet<>(interactedListingIds);

        // Stage 1: Fetch up to 200 from preferred province (The "Candidate" step)
        if (preferredProvinceCode != null) {
            log.info("User {} preferred province profiling: {}", userId, preferredProvinceCode);
            Integer pId = null;
            try { pId = Integer.parseInt(preferredProvinceCode); } catch (Exception ignored) {}

            List<Listing> localCandidates = listingRepository.findCandidatesForPersonalized(
                    pId, preferredProvinceCode, new ArrayList<>(excludedIds), PageRequest.of(0, 200));
            candidates.addAll(localCandidates);
            localCandidates.forEach(c -> excludedIds.add(c.getListingId()));
        }

        // Stage 2: Fetch globally to ensure pool has 300 candidates
        int globalSize = Math.max(100, 300 - candidates.size());
        List<Listing> globalCandidates = listingRepository.findCandidatesForPersonalizedGlobal(
                new ArrayList<>(excludedIds), PageRequest.of(0, globalSize));
        candidates.addAll(globalCandidates);

        if (candidates.isEmpty()) {
            return getColdStartFeed(topN);
        }

        // FIX #2: Build allInteractions from REAL global data (across all users) for CF
        // signal
        List<AIRecommendationRequest.InteractionEntryDto> allInteractions = new ArrayList<>(userInteractions);
        try {
            // Fetch recent 500 saves + 500 phone clicks from entire platform for CF signal
            for (SavedListing sl : savedListingRepository.findRecentGlobalSavedListings(PageRequest.of(0, 500))) {
                if (!sl.getId().getUserId().equals(userId)) {
                    allInteractions.add(new AIRecommendationRequest.InteractionEntryDto(
                            sl.getId().getUserId(), sl.getId().getListingId(), 3.0));
                }
            }
            for (PhoneClickDetail pc : phoneClickDetailRepository.findRecentGlobalPhoneClicks(PageRequest.of(0, 500))) {
                if (pc.getUser() != null && !pc.getUser().getUserId().equals(userId)) {
                    allInteractions.add(new AIRecommendationRequest.InteractionEntryDto(
                            pc.getUser().getUserId(), pc.getListing().getListingId(), 2.5));
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch global interactions for CF, falling back to user-only interactions", e);
        }

        // interactionFeatures already fetched above for profiling

        AIRecommendationRequest.PersonalizedFeedAiRequest aiRequest = AIRecommendationRequest.PersonalizedFeedAiRequest
                .builder()
                .user_id(userId)
                .user_interactions(userInteractions)
                .all_interactions(allInteractions)
                .candidates(candidates.stream().map(this::toListingFeatureDto).collect(Collectors.toList()))
                .top_n(topN)
                .alpha(0.5)
                .interactionFeatures(interactionFeatures)
                .build();

        try {
            List<RecommendationItemDto> recommendedItems = aiConnector.getPersonalizedFeed(aiRequest);
            return buildResponse(recommendedItems, "personalized", false, true);
        } catch (Exception e) {
            log.error("AI service error calling personalized API", e);
            return getColdStartFeed(topN);
        }
    }

    private RecommendationResponse getColdStartFeed(int topN) {
        List<Listing> candidates = listingRepository.findCandidatesForPersonalizedGlobal(List.of(-1L),
                PageRequest.of(0, 100));
        List<RecommendationItemDto> items = candidates.stream()
                .map(c -> RecommendationItemDto.builder().listingId(c.getListingId()).score(0.0).build())
                .collect(Collectors.toList());
        return buildResponse(items, "cold_start", true, true);
    }

    private RecommendationResponse buildResponse(List<RecommendationItemDto> recommendedItems, String mode,
            boolean isColdStart, boolean applyRanking) {
        if (recommendedItems.isEmpty()) {
            return RecommendationResponse.builder().listings(Collections.emptyList()).mode(mode).totalReturned(0)
                    .coldStart(isColdStart).build();
        }

        final double ALPHA = 0.6, BETA = 0.3, GAMMA = 0.1;

        Set<Long> listingIds = recommendedItems.stream().map(RecommendationItemDto::getListingId)
                .collect(Collectors.toSet());
        Map<Long, ListingResponse> listingsMap = listingService.getListingsByIds(listingIds).stream()
                .collect(Collectors.toMap(ListingResponse::getListingId, l -> l));

        List<ListingResponse> finalResponses = new ArrayList<>();

        if (applyRanking) {
            List<ScoredListing> scoredListings = new ArrayList<>();
            for (RecommendationItemDto item : recommendedItems) {
                ListingResponse res = listingsMap.get(item.getListingId());
                if (res != null) {
                    double adScore = getVipScore(res.getVipType());
                    double freshnessScore = calculateFreshnessScore(res);
                    double finalScore = (ALPHA * item.getScore()) + (BETA * adScore) + (GAMMA * freshnessScore);

                    res.setRecommendationScore(finalScore);
                    res.setPersonalizationScore(item.getCfScore());
                    res.setSimilarityScore(item.getCbfScore());
                    scoredListings.add(new ScoredListing(res, finalScore));
                }
            }
            scoredListings.sort((s1, s2) -> Double.compare(s2.score, s1.score));
            finalResponses = scoredListings.stream().map(s -> s.listing).collect(Collectors.toList());
        } else {
            for (RecommendationItemDto item : recommendedItems) {
                ListingResponse res = listingsMap.get(item.getListingId());
                if (res != null) {
                    res.setRecommendationScore(item.getScore());
                    res.setSimilarityScore(item.getCbfScore());
                    res.setPersonalizationScore(item.getCfScore());
                    finalResponses.add(res);
                }
            }
        }

        return RecommendationResponse.builder().listings(finalResponses).mode(mode).totalReturned(finalResponses.size())
                .coldStart(isColdStart).build();
    }

    private double getVipScore(String type) {
        if (type == null)
            return 0.2;
        return switch (type.toUpperCase()) {
            case "DIAMOND" -> 1.0;
            case "GOLD" -> 0.7;
            case "SILVER" -> 0.4;
            default -> 0.2;
        };
    }

    private double calculateFreshnessScore(ListingResponse res) {
        LocalDateTime date = res.getPushedAt() != null ? res.getPushedAt() : res.getPostDate();
        if (date == null)
            return 0.0;
        long days = ChronoUnit.DAYS.between(date, LocalDateTime.now());
        return 1.0 / (1.0 + Math.max(0, days));
    }

    @lombok.AllArgsConstructor
    private static class ScoredListing {
        ListingResponse listing;
        double score;
    }

    private AIRecommendationRequest.ListingFeatureDto toListingFeatureDto(Listing listing) {
        int days = 0;
        LocalDateTime date = listing.getPushedAt() != null ? listing.getPushedAt() : listing.getPostDate();
        if (date != null)
            days = (int) ChronoUnit.DAYS.between(date, LocalDateTime.now());

        // FIX: Ensure provinceCode is NEVER null for Pydantic/AI
        String pCode = "UNKNOWN";
        Integer dId = null;
        if (listing.getAddress() != null) {
            var addr = listing.getAddress();
            if (addr.getNewProvinceCode() != null && !addr.getNewProvinceCode().isEmpty()) {
                pCode = addr.getNewProvinceCode();
            } else if (addr.getLegacyProvinceId() != null) {
                pCode = String.valueOf(addr.getLegacyProvinceId());
            }
            dId = addr.getLegacyDistrictId();
        }

        return AIRecommendationRequest.ListingFeatureDto.builder()
                .listingId(listing.getListingId())
                .productType(listing.getProductType() != null ? listing.getProductType().name() : "ROOM")
                .listingType(listing.getListingType() != null ? listing.getListingType().name() : "RENT")
                .price(listing.getPrice() != null ? listing.getPrice().doubleValue() : 0.0)
                .area(listing.getArea() != null ? listing.getArea() : 0.0)
                .bedrooms(listing.getBedrooms() != null ? listing.getBedrooms() : 0)
                .provinceCode(pCode)
                .districtId(dId)
                .vipType(listing.getVipType() != null ? listing.getVipType().name() : "NORMAL")
                .postDateDaysAgo(Math.max(0, days))
                .build();
    }
}
