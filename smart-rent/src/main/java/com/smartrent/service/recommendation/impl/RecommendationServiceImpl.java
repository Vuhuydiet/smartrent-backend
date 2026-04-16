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

    // ─────────────────────────────────────────────
    // PUBLIC: Similar Listings
    // ─────────────────────────────────────────────

    @Override
    public RecommendationResponse getSimilarListings(Long listingId, int topN, String userId) {
        Listing target = listingRepository.findByIdWithAddress(listingId).orElse(null);
        if (target == null) {
            return emptyResponse("similar");
        }

        com.smartrent.infra.repository.entity.Address targetAddr = target.getAddress();
        Integer provinceId = (targetAddr != null) ? targetAddr.getLegacyProvinceId() : null;
        String provinceCode = (targetAddr != null) ? targetAddr.getNewProvinceCode() : null;

        List<Listing> candidates;
        if (provinceId != null || provinceCode != null) {
            candidates = listingRepository.findCandidatesForSimilar(
                    provinceId, provinceCode,
                    target.getProductType(), target.getListingType(),
                    listingId, PageRequest.of(0, 200));
        } else {
            candidates = listingRepository.findCandidatesForSimilarGlobal(
                    target.getProductType(), target.getListingType(),
                    listingId, PageRequest.of(0, 200));
        }

        if (candidates.isEmpty()) {
            return emptyResponse("similar");
        }

        // Build user interaction signals (optional – null for anonymous)
        List<AIRecommendationRequest.InteractionEntryDto> userInteractions = null;
        Set<Long> seenIds = new HashSet<>();
        if (userId != null) {
            userInteractions = new ArrayList<>();
            for (SavedListing s : savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
                userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, s.getId().getListingId(), 3.0));
                seenIds.add(s.getId().getListingId());
            }
            for (Long lId : phoneClickDetailRepository.findListingIdsByUserId(userId)) {
                userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, lId, 2.5));
                seenIds.add(lId);
            }
            for (RecentlyViewedItemResponse rv : recentlyViewedService.getRecentlyViewed(userId)) {
                Long lId = rv.getListing().getListingId();
                if (!seenIds.contains(lId)) {
                    userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, lId, 1.0));
                    seenIds.add(lId);
                }
            }
        }

        List<AIRecommendationRequest.ListingFeatureDto> interactionFeatures = null;
        if (userId != null && !seenIds.isEmpty()) {
            interactionFeatures = listingRepository.findWithAddressByListingIds(seenIds).stream()
                    .map(this::toFeatureDto).collect(Collectors.toList());
        }

        AIRecommendationRequest.SimilarListingAiRequest aiRequest =
                AIRecommendationRequest.SimilarListingAiRequest.builder()
                        .target(toFeatureDto(target))
                        .candidates(candidates.stream().map(this::toFeatureDto).collect(Collectors.toList()))
                        .top_n(topN)
                        .alpha(0.6)
                        .userInteractions(userInteractions)
                        .interactionFeatures(interactionFeatures)
                        .build();

        try {
            List<RecommendationItemDto> result = aiConnector.getSimilarListings(aiRequest);
            return buildResponse(result, userId != null ? "similar_personalized" : "similar", false, false);
        } catch (Exception e) {
            log.error("AI service error for similar listings (listingId={})", listingId, e);
            List<RecommendationItemDto> fallback = candidates.stream().limit(topN)
                    .map(c -> RecommendationItemDto.builder().listingId(c.getListingId()).score(0.0).build())
                    .collect(Collectors.toList());
            return buildResponse(fallback, "similar_fallback", true, false);
        }
    }

    // ─────────────────────────────────────────────
    // AUTHENTICATED: Personalized Feed
    // ─────────────────────────────────────────────

    @Override
    public RecommendationResponse getPersonalizedFeed(String userId, int topN) {
        // 1. Deduplicate interactions — keep MAX weight per listing
        Map<Long, Double> interactionWeightMap = new LinkedHashMap<>();

        for (SavedListing s : savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            interactionWeightMap.merge(s.getId().getListingId(), 3.0, Math::max);
        }
        for (Long lId : phoneClickDetailRepository.findListingIdsByUserId(userId)) {
            interactionWeightMap.merge(lId, 2.5, Math::max);
        }
        for (RecentlyViewedItemResponse rv : recentlyViewedService.getRecentlyViewed(userId)) {
            interactionWeightMap.merge(rv.getListing().getListingId(), 1.0, Math::max);
        }

        if (interactionWeightMap.isEmpty()) {
            return getColdStartFeed(topN);
        }

        List<AIRecommendationRequest.InteractionEntryDto> userInteractions = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : interactionWeightMap.entrySet()) {
            userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, entry.getKey(), entry.getValue()));
        }

        // 2. Feature extraction for user's interacted listings (for profile building)
        Set<Long> interactedListingIds = interactionWeightMap.keySet();
        List<Listing> interactedListings = listingRepository.findWithAddressByListingIds(interactedListingIds);
        List<AIRecommendationRequest.ListingFeatureDto> interactionFeatures = interactedListings.stream()
                .map(this::toFeatureDto).collect(Collectors.toList());

        // 3. Location profiling: pick most frequent province from interactions
        String preferredProvinceCode = interactionFeatures.stream()
                .filter(f -> f.getProvinceCode() != null && !f.getProvinceCode().equals("UNKNOWN"))
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getProvinceCode, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (preferredProvinceCode != null) {
            log.info("[Recommendation] User {} → preferred province: {}", userId, preferredProvinceCode);
        }

        // 4. Hybrid candidate pool (local-first)
        List<Listing> candidates = new ArrayList<>();
        Set<Long> excludedIds = new HashSet<>(interactedListingIds);

        // Stage 1: up to 200 from preferred province
        if (preferredProvinceCode != null) {
            Integer pId = null;
            try { pId = Integer.parseInt(preferredProvinceCode); } catch (Exception ignored) {}
            List<Listing> local = listingRepository.findCandidatesForPersonalized(
                    pId, preferredProvinceCode, new ArrayList<>(excludedIds), PageRequest.of(0, 200));
            candidates.addAll(local);
            local.forEach(c -> excludedIds.add(c.getListingId()));
        }

        // Stage 2: global top-up to reach ~300 candidates
        int globalSize = Math.max(100, 300 - candidates.size());
        candidates.addAll(listingRepository.findCandidatesForPersonalizedGlobal(
                new ArrayList<>(excludedIds), PageRequest.of(0, globalSize)));

        if (candidates.isEmpty()) {
            return getColdStartFeed(topN);
        }

        // 5. Build all_interactions for CF signal (recent global data)
        List<AIRecommendationRequest.InteractionEntryDto> allInteractions = new ArrayList<>(userInteractions);
        try {
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
            log.warn("[Recommendation] Could not fetch global interactions for CF signal", e);
        }

        AIRecommendationRequest.PersonalizedFeedAiRequest aiRequest =
                AIRecommendationRequest.PersonalizedFeedAiRequest.builder()
                        .user_id(userId)
                        .user_interactions(userInteractions)
                        .all_interactions(allInteractions)
                        .candidates(candidates.stream().map(this::toFeatureDto).collect(Collectors.toList()))
                        .top_n(topN)
                        .alpha(0.5)
                        .interactionFeatures(interactionFeatures)
                        .build();

        try {
            List<RecommendationItemDto> result = aiConnector.getPersonalizedFeed(aiRequest);
            return buildResponse(result, "personalized", false, true);
        } catch (Exception e) {
            log.error("[Recommendation] AI service error for personalized feed (userId={})", userId, e);
            return getColdStartFeed(topN);
        }
    }

    // ─────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────

    private RecommendationResponse getColdStartFeed(int topN) {
        List<Listing> candidates = listingRepository.findCandidatesForPersonalizedGlobal(
                List.of(-1L), PageRequest.of(0, Math.max(topN, 100)));
        List<RecommendationItemDto> items = candidates.stream()
                .map(c -> RecommendationItemDto.builder().listingId(c.getListingId()).score(0.0).build())
                .collect(Collectors.toList());
        return buildResponse(items, "cold_start", true, true);
    }

    private RecommendationResponse emptyResponse(String mode) {
        return RecommendationResponse.builder()
                .listings(Collections.emptyList())
                .mode(mode)
                .totalReturned(0)
                .coldStart(false)
                .build();
    }

    /**
     * Build final response: fetch full ListingResponse for each recommended id,
     * optionally re-rank using personalization + ad score + freshness.
     */
    private RecommendationResponse buildResponse(
            List<RecommendationItemDto> items, String mode, boolean isColdStart, boolean applyRanking) {

        if (items.isEmpty()) {
            return RecommendationResponse.builder()
                    .listings(Collections.emptyList())
                    .mode(mode).totalReturned(0).coldStart(isColdStart).build();
        }

        final double ALPHA = 0.6, BETA = 0.3, GAMMA = 0.1;

        Set<Long> ids = items.stream().map(RecommendationItemDto::getListingId).collect(Collectors.toSet());
        Map<Long, ListingResponse> listingMap = listingService.getListingsByIds(ids).stream()
                .collect(Collectors.toMap(ListingResponse::getListingId, l -> l));

        List<ListingResponse> finalList;

        if (applyRanking) {
            List<ScoredListing> scored = new ArrayList<>();
            for (RecommendationItemDto item : items) {
                ListingResponse res = listingMap.get(item.getListingId());
                if (res != null) {
                    double adScore = vipScore(res.getVipType());
                    double freshScore = freshnessScore(res);
                    double finalScore = (ALPHA * item.getScore()) + (BETA * adScore) + (GAMMA * freshScore);
                    res.setRecommendationScore(finalScore);
                    res.setPersonalizationScore(item.getCfScore());
                    res.setSimilarityScore(item.getCbfScore());
                    scored.add(new ScoredListing(res, finalScore));
                }
            }
            scored.sort((a, b) -> Double.compare(b.score, a.score));
            finalList = scored.stream().map(s -> s.listing).collect(Collectors.toList());
        } else {
            finalList = new ArrayList<>();
            for (RecommendationItemDto item : items) {
                ListingResponse res = listingMap.get(item.getListingId());
                if (res != null) {
                    res.setRecommendationScore(item.getScore());
                    res.setSimilarityScore(item.getCbfScore());
                    res.setPersonalizationScore(item.getCfScore());
                    finalList.add(res);
                }
            }
        }

        return RecommendationResponse.builder()
                .listings(finalList)
                .mode(mode)
                .totalReturned(finalList.size())
                .coldStart(isColdStart)
                .build();
    }

    private double vipScore(String type) {
        if (type == null) return 0.2;
        return switch (type.toUpperCase()) {
            case "DIAMOND" -> 1.0;
            case "GOLD"    -> 0.7;
            case "SILVER"  -> 0.4;
            default        -> 0.2;
        };
    }

    private double freshnessScore(ListingResponse res) {
        LocalDateTime date = res.getPushedAt() != null ? res.getPushedAt() : res.getPostDate();
        if (date == null) return 0.0;
        long days = ChronoUnit.DAYS.between(date, LocalDateTime.now());
        return 1.0 / (1.0 + Math.max(0, days));
    }

    private AIRecommendationRequest.ListingFeatureDto toFeatureDto(Listing listing) {
        int daysAgo = 0;
        LocalDateTime date = listing.getPushedAt() != null ? listing.getPushedAt() : listing.getPostDate();
        if (date != null) daysAgo = (int) ChronoUnit.DAYS.between(date, LocalDateTime.now());

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
                .postDateDaysAgo(Math.max(0, daysAgo))
                .build();
    }

    @lombok.AllArgsConstructor
    private static class ScoredListing {
        ListingResponse listing;
        double score;
    }
}
