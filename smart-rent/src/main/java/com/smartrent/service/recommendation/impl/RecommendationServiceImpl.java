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
import com.smartrent.infra.repository.AddressMappingRepository;
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
    AddressMappingRepository addressMappingRepository;
    com.smartrent.service.recommendation.RecommendationExecutor recommendationExecutor;

    // Memoizes legacy→new address-mapping lookups (static reference data) to
    // collapse the per-candidate N+1 in toFeatureDto when new codes are missing.
    // Final + initialized → excluded from the @RequiredArgsConstructor.
    Map<String, Optional<com.smartrent.infra.repository.entity.AddressMapping>> legacyMappingCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    // ─────────────────────────────────────────────
    // PUBLIC: Similar Listings
    // ─────────────────────────────────────────────

    @Override
    @org.springframework.cache.annotation.Cacheable(cacheNames = com.smartrent.config.Constants.CacheNames.LISTING_RECOMMENDATION_SIMILAR, key = "'listing:' + #listingId + ':topN:' + #topN + ':user:' + (#userId != null ? #userId : 'anonymous')", unless = "#result == null || #result.listings == null || #result.listings.isEmpty()")
    public RecommendationResponse getSimilarListings(Long listingId, int topN, String userId) {
        Listing target = listingRepository.findByIdWithAddress(listingId).orElse(null);
        if (target == null) {
            return emptyResponse("similar");
        }

        long tStart = System.nanoTime();
        com.smartrent.infra.repository.entity.Address targetAddr = target.getAddress();
        Integer provinceId = (targetAddr != null) ? targetAddr.getLegacyProvinceId() : null;
        String provinceCode = (targetAddr != null) ? targetAddr.getNewProvinceCode() : null;

        java.util.concurrent.CompletableFuture<List<Listing>> proximityFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    Integer districtId = (targetAddr != null) ? targetAddr.getLegacyDistrictId() : null;
                    if (districtId != null) {
                        return listingRepository.findSimilarProximityCandidates(
                                provinceCode, provinceId, districtId, target.getProductType(), target.getListingType(),
                                listingId, PageRequest.of(0, 150));
                    }
                    return java.util.Collections.emptyList();
                });

        java.util.concurrent.CompletableFuture<List<Listing>> priceFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    java.math.BigDecimal price = target.getPrice();
                    if (price != null) {
                        java.math.BigDecimal minPrice = price.multiply(java.math.BigDecimal.valueOf(0.8));
                        java.math.BigDecimal maxPrice = price.multiply(java.math.BigDecimal.valueOf(1.2));
                        return listingRepository.findSimilarPriceCandidates(
                                provinceCode, provinceId, minPrice, maxPrice, target.getProductType(),
                                target.getListingType(),
                                listingId, PageRequest.of(0, 120));
                    }
                    return java.util.Collections.emptyList();
                });

        java.util.concurrent.CompletableFuture<List<Listing>> freshFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    if (provinceCode != null && !provinceCode.isEmpty() && !provinceCode.equals("UNKNOWN")) {
                        return listingRepository.findCandidatesForSimilarByNewProvince(
                                provinceCode, target.getProductType(), target.getListingType(),
                                listingId, PageRequest.of(0, 100));
                    } else if (provinceId != null) {
                        return listingRepository.findCandidatesForSimilarByLegacyProvince(
                                provinceId, target.getProductType(), target.getListingType(),
                                listingId, PageRequest.of(0, 100));
                    } else {
                        return listingRepository.findCandidatesForSimilarGlobal(
                                target.getProductType(), target.getListingType(),
                                listingId, PageRequest.of(0, 100));
                    }
                });

        List<Listing> candidates;
        try {
            java.util.concurrent.CompletableFuture.allOf(proximityFuture, priceFuture, freshFuture).join();

            Set<Listing> candidateSet = new LinkedHashSet<>();
            candidateSet.addAll(proximityFuture.get());
            candidateSet.addAll(priceFuture.get());
            candidateSet.addAll(freshFuture.get());

            // Stage-2 Fallback Top-up if candidate pool is less than 120 (prevent candidate
            // starvation). Pool trimmed 300→200 for cold-start latency: on the 32MB
            // buffer pool, hydrating fewer candidates cuts the cold disk reads; 200 is
            // ample to rank a top-N feed. (Similar has no discovery-shift pinning, so
            // unlike the personalized feed it does not need a large fixed pool.)
            if (candidateSet.size() < 120) {
                int needed = 200 - candidateSet.size();
                List<Listing> topUp;
                if (provinceCode != null && !provinceCode.isEmpty() && !provinceCode.equals("UNKNOWN")) {
                    topUp = listingRepository.findCandidatesForSimilarByNewProvince(
                            provinceCode, target.getProductType(), target.getListingType(),
                            listingId, PageRequest.of(0, needed));
                } else if (provinceId != null) {
                    topUp = listingRepository.findCandidatesForSimilarByLegacyProvince(
                            provinceId, target.getProductType(), target.getListingType(),
                            listingId, PageRequest.of(0, needed));
                } else {
                    topUp = listingRepository.findCandidatesForSimilarGlobal(
                            target.getProductType(), target.getListingType(),
                            listingId, PageRequest.of(0, needed));
                }
                candidateSet.addAll(topUp);
            }

            candidates = candidateSet.stream()
                    .limit(200)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error executing parallel multi-channel retrieval for similar listings", e);
            candidates = freshFuture.join();
        }

        long tAfterCandidates = System.nanoTime();

        if (candidates.isEmpty()) {
            return emptyResponse("similar");
        }

        // Build user interaction signals (optional – null for anonymous)
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
            for (Long lId : recentlyViewedService.getRecentlyViewedIds(userId)) {
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

        AIRecommendationRequest.SimilarListingAiRequest aiRequest = AIRecommendationRequest.SimilarListingAiRequest
                .builder()
                .target(toFeatureDto(target))
                .candidates(candidates.stream().map(this::toFeatureDto).collect(Collectors.toList()))
                .top_n(topN)
                .userInteractions(userInteractions)
                .interactionFeatures(interactionFeatures)
                .build();

        long tBeforeAi = System.nanoTime();
        try {
            List<RecommendationItemDto> result = aiConnector.getSimilarListings(aiRequest);
            long tAfterAi = System.nanoTime();
            log.info("[PerfTrace] similar listingId={} candidatePool={} | candidateRetrieval={}ms interactions+features={}ms feignAi={}ms total(soFar)={}ms",
                    listingId, candidates.size(),
                    (tAfterCandidates - tStart) / 1_000_000,
                    (tBeforeAi - tAfterCandidates) / 1_000_000,
                    (tAfterAi - tBeforeAi) / 1_000_000,
                    (tAfterAi - tStart) / 1_000_000);
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
    @org.springframework.cache.annotation.Cacheable(cacheNames = com.smartrent.config.Constants.CacheNames.LISTING_RECOMMENDATION_PERSONALIZED, key = "'user:' + #userId + ':topN:' + #topN", unless = "#result == null || #result.listings == null || #result.listings.isEmpty()")
    public RecommendationResponse getPersonalizedFeed(String userId, int topN) {
        long tStart = System.nanoTime();
        Map<Long, Double> interactionWeightMap = new LinkedHashMap<>();
        List<SavedListing> savedListings = savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long tAfterSaved = System.nanoTime();

        List<Long> clickedListingIds = phoneClickDetailRepository.findListingIdsByUserId(userId);
        long tAfterClicks = System.nanoTime();

        // IDs-only recency read (no full-DTO hydration): this path only needs the
        // viewed listing IDs and their order below, so getRecentlyViewedIds skips
        // the getDisplayingListingsByIds hydration of up to 20 listings that
        // getRecentlyViewed() would run.
        List<Long> viewedListingIds = recentlyViewedService.getRecentlyViewedIds(userId);
        long tAfterViewed = System.nanoTime();

        for (SavedListing s : savedListings) {
            interactionWeightMap.merge(s.getId().getListingId(), 3.0, Math::max);
        }
        for (Long lId : clickedListingIds) {
            interactionWeightMap.merge(lId, 2.5, Math::max);
        }
        for (Long lId : viewedListingIds) {
            interactionWeightMap.merge(lId, 1.0, Math::max);
        }

        boolean hasEnoughInteractions = !savedListings.isEmpty() || !clickedListingIds.isEmpty()
                || viewedListingIds.size() >= 3;
        if (!hasEnoughInteractions) {
            return getColdStartFeed(topN);
        }

        List<AIRecommendationRequest.InteractionEntryDto> userInteractions = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : interactionWeightMap.entrySet()) {
            userInteractions
                    .add(new AIRecommendationRequest.InteractionEntryDto(userId, entry.getKey(), entry.getValue()));
        }

        // 2. Feature extraction for user's interacted listings (for profile building)
        Set<Long> interactedListingIds = interactionWeightMap.keySet();
        List<Listing> interactedListings = listingRepository.findWithAddressByListingIds(interactedListingIds);

        // Sort interactedListings so that the most recently viewed items come FIRST.
        // viewedListingIds is already in recency order (most recent first) from Redis.
        List<Long> recencyOrder = viewedListingIds;

        interactedListings.sort((l1, l2) -> {
            int idx1 = recencyOrder.indexOf(l1.getListingId());
            int idx2 = recencyOrder.indexOf(l2.getListingId());
            // If not in recent views, treat as older (push to back)
            if (idx1 == -1)
                idx1 = Integer.MAX_VALUE;
            if (idx2 == -1)
                idx2 = Integer.MAX_VALUE;
            return Integer.compare(idx1, idx2);
        });

        List<AIRecommendationRequest.ListingFeatureDto> interactionFeatures = interactedListings.stream()
                .map(this::toFeatureDto).collect(Collectors.toList());
        long tAfterInteractions = System.nanoTime();
        log.info("[PerfTrace] personalized interactions breakdown userId={} | saved={}ms clicks={}ms recentlyViewed={}ms featureExtract={}ms",
                userId,
                (tAfterSaved - tStart) / 1_000_000,
                (tAfterClicks - tAfterSaved) / 1_000_000,
                (tAfterViewed - tAfterClicks) / 1_000_000,
                (tAfterInteractions - tAfterViewed) / 1_000_000);

        // 3. Location profiling
        // 3.1 Find most frequent (preferred) locations
        String preferredProvinceCode = interactionFeatures.stream()
                .filter(f -> f.getProvinceCode() != null && !f.getProvinceCode().equals("UNKNOWN"))
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getProvinceCode,
                        Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        String preferredWardCode = interactionFeatures.stream()
                .filter(f -> f.getNewWardCode() != null)
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getNewWardCode,
                        Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Integer preferredDistrictId = interactionFeatures.stream()
                .filter(f -> f.getDistrictId() != null)
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getDistrictId,
                        Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Integer preferredWardId = interactionFeatures.stream()
                .filter(f -> f.getWardId() != null)
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getWardId,
                        Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Double preferredLat = null;
        Double preferredLon = null;
        if (interactionFeatures != null && !interactionFeatures.isEmpty()) {
            List<AIRecommendationRequest.ListingFeatureDto> coords = interactionFeatures.stream()
                    .filter(f -> f.getLatitude() != null && f.getLatitude() > 0 && f.getLongitude() != null && f.getLongitude() > 0)
                    .collect(Collectors.toList());
            if (!coords.isEmpty()) {
                preferredLat = coords.stream().mapToDouble(AIRecommendationRequest.ListingFeatureDto::getLatitude).average().orElse(0.0);
                preferredLon = coords.stream().mapToDouble(AIRecommendationRequest.ListingFeatureDto::getLongitude).average().orElse(0.0);
            }
        }

        // 3.1.2 Calculate condition counts for Discovery Shift triggering.
        // Spec: shift only when the user has explored a NEW place — 3 views OR
        // 2 saves OR 2 phone clicks whose location differs from the preferred one.
        // All three are measured against the preferred location with the same
        // toFeatureDto representation (new province/ward code + legacy district/ward
        // id) the rest of the discovery flow uses, so legacy/new codes compare like
        // for like instead of silently mismatching.
        Map<Long, AIRecommendationRequest.ListingFeatureDto> featureById = new HashMap<>();
        for (AIRecommendationRequest.ListingFeatureDto f : interactionFeatures) {
            featureById.put(f.getListingId(), f);
        }

        long diffLocViews = viewedListingIds.stream()
                .distinct()
                .filter(id -> isDifferentFromPreferred(featureById.get(id),
                        preferredProvinceCode, preferredDistrictId, preferredWardId, preferredWardCode))
                .count();

        long diffLocSaved = savedListings.stream()
                .map(s -> s.getId().getListingId())
                .filter(id -> isDifferentFromPreferred(featureById.get(id),
                        preferredProvinceCode, preferredDistrictId, preferredWardId, preferredWardCode))
                .count();

        long diffLocPhoneClicks = clickedListingIds.stream()
                .filter(id -> isDifferentFromPreferred(featureById.get(id),
                        preferredProvinceCode, preferredDistrictId, preferredWardId, preferredWardCode))
                .count();

        boolean meetsShiftCondition = (diffLocViews >= 3) || (diffLocSaved >= 2) || (diffLocPhoneClicks >= 2);
        log.info("[Discovery Shift] user={} diffLocViews={} diffLocSaved={} diffLocPhoneClicks={} meetsShiftCondition={}",
                userId, diffLocViews, diffLocSaved, diffLocPhoneClicks, meetsShiftCondition);

        // 3.2 Find the Discovery Zone: the most-recent interaction that sits in a
        // NEW place (different from preferred). Anchoring the zone to a
        // different-place listing keeps it consistent with meetsShiftCondition — so
        // whenever the shift triggers we pin listings from the place the user is
        // actually exploring, not whichever listing they happened to touch last.
        // Scan by recency: views (recency-ordered) → saves (createdAt desc) → clicks.
        // Fall back to the plain latest interaction when nothing differs (isShift
        // stays false in that case).
        Long latestListingId = null;
        if (!viewedListingIds.isEmpty()) {
            latestListingId = viewedListingIds.get(0);
        } else if (!savedListings.isEmpty()) {
            latestListingId = savedListings.get(0).getId().getListingId();
        } else if (!clickedListingIds.isEmpty()) {
            latestListingId = clickedListingIds.get(0);
        }

        String discoveryWardCode = null;
        Integer discoveryWardId = null;
        Integer discoveryDistrictId = null;
        String discoveryProvinceCode = null;

        if (latestListingId != null) {
            Listing latestListing = listingRepository.findByIdWithAddress(latestListingId).orElse(null);
            if (latestListing != null) {
                AIRecommendationRequest.ListingFeatureDto latestFeature = toFeatureDto(latestListing);
                discoveryProvinceCode = latestFeature.getProvinceCode();
                discoveryWardCode = latestFeature.getNewWardCode();
                discoveryWardId = latestFeature.getWardId();
                discoveryDistrictId = latestFeature.getDistrictId();
            }
        }
        log.info("[Recommendation Test] latestListingId = {}, discoveryProvinceCode = {}, preferredProvinceCode = {}",
                latestListingId, discoveryProvinceCode, preferredProvinceCode);

        boolean isShift = false;
        if (meetsShiftCondition) {
            if (discoveryProvinceCode != null && preferredProvinceCode != null
                    && !discoveryProvinceCode.equals(preferredProvinceCode)) {
                isShift = true;
            } else if (discoveryDistrictId != null && preferredDistrictId != null
                    && !discoveryDistrictId.equals(preferredDistrictId)) {
                isShift = true;
            } else if (discoveryWardCode != null && preferredWardCode != null
                    && !discoveryWardCode.equals(preferredWardCode)) {
                isShift = true;
            } else if (discoveryWardId != null && preferredWardId != null && !discoveryWardId.equals(preferredWardId)) {
                isShift = true;
            }
        }

        final Integer finalDiscoveryDistrictId = discoveryDistrictId;
        final String finalDiscoveryWardCode = discoveryWardCode;
        final Integer finalDiscoveryWardId = discoveryWardId;


        java.util.concurrent.CompletableFuture<List<Listing>> proximityFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    List<Listing> list = new ArrayList<>();
                    List<Long> threadExclusions = new ArrayList<>(interactedListingIds);
                    if (threadExclusions.isEmpty())
                        threadExclusions.add(-1L);

                    // Preferred location (up to 150)
                    if (preferredDistrictId != null) {
                        list.addAll(listingRepository.findCandidatesByDistrict(
                                preferredDistrictId, threadExclusions, PageRequest.of(0, 150)));
                    } else if (preferredWardCode != null && !preferredWardCode.isEmpty()) {
                        list.addAll(listingRepository.findCandidatesByNewWard(
                                preferredWardCode, threadExclusions, PageRequest.of(0, 150)));
                    } else if (preferredWardId != null) {
                        list.addAll(listingRepository.findCandidatesByLegacyWard(
                                preferredWardId, threadExclusions, PageRequest.of(0, 150)));
                    }

                    // Discovery shift location (up to 100)
                    if (finalDiscoveryDistrictId != null) {
                        list.addAll(listingRepository.findCandidatesByDistrict(
                                finalDiscoveryDistrictId, threadExclusions, PageRequest.of(0, 100)));
                    } else if (finalDiscoveryWardCode != null && !finalDiscoveryWardCode.isEmpty()) {
                        list.addAll(listingRepository.findCandidatesByNewWard(
                                finalDiscoveryWardCode, threadExclusions, PageRequest.of(0, 100)));
                    } else if (finalDiscoveryWardId != null) {
                        list.addAll(listingRepository.findCandidatesByLegacyWard(
                                finalDiscoveryWardId, threadExclusions, PageRequest.of(0, 100)));
                    }
                    return list;
                }, recommendationExecutor.pool());

        java.util.concurrent.CompletableFuture<List<Listing>> priceFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    List<Long> threadExclusions = new ArrayList<>(interactedListingIds);
                    if (threadExclusions.isEmpty())
                        threadExclusions.add(-1L);

                    double avgPrice = interactionFeatures.stream()
                            .mapToDouble(AIRecommendationRequest.ListingFeatureDto::getPrice)
                            .average().orElse(5000000.0);
                    java.math.BigDecimal minPrice = java.math.BigDecimal.valueOf(avgPrice * 0.8);
                    java.math.BigDecimal maxPrice = java.math.BigDecimal.valueOf(avgPrice * 1.2);

                    Integer pId = null;
                    if (preferredProvinceCode != null) {
                        try {
                            pId = Integer.parseInt(preferredProvinceCode);
                        } catch (Exception ignored) {
                        }
                    }
                    return listingRepository.findPersonalizedPriceCandidates(
                            preferredProvinceCode, pId, minPrice, maxPrice, threadExclusions, PageRequest.of(0, 200));
                }, recommendationExecutor.pool());

        java.util.concurrent.CompletableFuture<List<Listing>> freshFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> {
                    List<Listing> list = new ArrayList<>();
                    List<Long> threadExclusions = new ArrayList<>(interactedListingIds);
                    if (threadExclusions.isEmpty())
                        threadExclusions.add(-1L);

                    if (preferredProvinceCode != null && !preferredProvinceCode.isEmpty()
                            && !preferredProvinceCode.equals("UNKNOWN")) {
                        list.addAll(listingRepository.findCandidatesForPersonalizedByNewProvince(
                                preferredProvinceCode, threadExclusions, PageRequest.of(0, 100)));
                    } else {
                        Integer pId = null;
                        if (preferredProvinceCode != null) {
                            try {
                                pId = Integer.parseInt(preferredProvinceCode);
                            } catch (Exception ignored) {
                            }
                        }
                        if (pId != null) {
                            list.addAll(listingRepository.findCandidatesForPersonalizedByLegacyProvince(
                                    pId, threadExclusions, PageRequest.of(0, 100)));
                        }
                    }
                    // No inline global top-up: the global pool query is a
                    // full-table scan + filesort (no index covers its
                    // public-visibility booleans + pushed_at/post_date sort), and
                    // it dominated the cold-path latency. The stage-2 starvation
                    // top-up below already fetches the global pool, but ONLY when
                    // the combined proximity+price+province pool is < 150 — so the
                    // common path (users with enough local candidates) no longer
                    // pays for it.
                    return list;
                }, recommendationExecutor.pool());

        List<Listing> candidates;
        try {
            java.util.concurrent.CompletableFuture.allOf(proximityFuture, priceFuture, freshFuture).join();

            Set<Listing> candidateSet = new LinkedHashSet<>();
            candidateSet.addAll(proximityFuture.get());
            candidateSet.addAll(priceFuture.get());
            candidateSet.addAll(freshFuture.get());

            // Stage-2 Fallback Top-up if candidate pool is less than 150 (prevent candidate
            // starvation). Priority order: preferred province → discovery province → global.
            // Going straight to global (ORDER BY pushedAt DESC, no province filter) was the
            // root cause of HCM VIP listings flooding the pool when the preferred zone
            // (e.g. tỉnh Bình Dương) has few listings, making them dominate the top slots.
            if (candidateSet.size() < 150) {
                List<Long> threadExclusions = new ArrayList<>(interactedListingIds);
                candidateSet.forEach(l -> threadExclusions.add(l.getListingId()));
                if (threadExclusions.isEmpty()) threadExclusions.add(-1L);

                // 1. Preferred province top-up
                if (candidateSet.size() < 150 && preferredProvinceCode != null
                        && !preferredProvinceCode.isEmpty() && !preferredProvinceCode.equals("UNKNOWN")) {
                    int need = 300 - candidateSet.size();
                    try {
                        Integer pId = null;
                        try { pId = Integer.parseInt(preferredProvinceCode); } catch (Exception ignored) {}
                        if (pId != null) {
                            candidateSet.addAll(listingRepository.findCandidatesForPersonalizedByLegacyProvince(
                                    pId, threadExclusions, PageRequest.of(0, need)));
                        } else {
                            candidateSet.addAll(listingRepository.findCandidatesForPersonalizedByNewProvince(
                                    preferredProvinceCode, threadExclusions, PageRequest.of(0, need)));
                        }
                        candidateSet.forEach(l -> { if (!threadExclusions.contains(l.getListingId())) threadExclusions.add(l.getListingId()); });
                    } catch (Exception ignored) {}
                }

                // 2. Discovery province top-up (only when shift is active)
                if (candidateSet.size() < 150 && isShift && discoveryProvinceCode != null
                        && !discoveryProvinceCode.isEmpty() && !discoveryProvinceCode.equals("UNKNOWN")) {
                    int need = 300 - candidateSet.size();
                    try {
                        Integer dPId = null;
                        try { dPId = Integer.parseInt(discoveryProvinceCode); } catch (Exception ignored) {}
                        if (dPId != null) {
                            candidateSet.addAll(listingRepository.findCandidatesForPersonalizedByLegacyProvince(
                                    dPId, threadExclusions, PageRequest.of(0, need)));
                        } else {
                            candidateSet.addAll(listingRepository.findCandidatesForPersonalizedByNewProvince(
                                    discoveryProvinceCode, threadExclusions, PageRequest.of(0, need)));
                        }
                        candidateSet.forEach(l -> { if (!threadExclusions.contains(l.getListingId())) threadExclusions.add(l.getListingId()); });
                    } catch (Exception ignored) {}
                }

                // 3. Global top-up as last resort only if still starved
                if (candidateSet.size() < 50) {
                    int needed = 300 - candidateSet.size();
                    List<Listing> topUp = listingRepository.findCandidatesForPersonalizedGlobal(
                            threadExclusions, PageRequest.of(0, needed));
                    candidateSet.addAll(topUp);
                }
            }


            candidates = candidateSet.stream()
                    .limit(300)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error executing parallel multi-channel retrieval for personalized feed", e);
            candidates = freshFuture.join();
        }

        long tAfterCandidates = System.nanoTime();

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

        List<AIRecommendationRequest.ListingFeatureDto> distinctCandidates = new ArrayList<>();
        Set<Long> seenCandidateIds = new HashSet<>();
        for (Listing c : candidates) {
            if (seenCandidateIds.add(c.getListingId())) {
                distinctCandidates.add(toFeatureDto(c));
            }
        }

        // When discovery shift pinning will run, ask AI for 3× the items so the
        // Java post-processing step has enough preferred-zone candidates to fill
        // the top 7 slots after pinning 3 discovery slots.
        int aiTopN = isShift ? Math.min(topN * 3, distinctCandidates.size()) : topN;

        AIRecommendationRequest.PersonalizedFeedAiRequest aiRequest = AIRecommendationRequest.PersonalizedFeedAiRequest
                .builder()
                .user_id(userId)
                .user_interactions(userInteractions)
                .all_interactions(allInteractions)
                .candidates(distinctCandidates)
                .top_n(aiTopN)
                .interactionFeatures(interactionFeatures)
                .meetsShiftCondition(meetsShiftCondition)
                .build();

        try {
            long tBeforeAi = System.nanoTime();
            List<RecommendationItemDto> result = aiConnector.getPersonalizedFeed(aiRequest);
            long tAfterAi = System.nanoTime();
            log.info("[PerfTrace] personalized userId={} candidatePool={} | interactions={}ms candidateRetrieval={}ms cfGlobal+features={}ms feignAi={}ms total(soFar)={}ms",
                    userId, candidates.size(),
                    (tAfterInteractions - tStart) / 1_000_000,
                    (tAfterCandidates - tAfterInteractions) / 1_000_000,
                    (tBeforeAi - tAfterCandidates) / 1_000_000,
                    (tAfterAi - tBeforeAi) / 1_000_000,
                    (tAfterAi - tStart) / 1_000_000);
            // applyRanking=false: trust the AI service's hybrid score (which already
            // includes the tuned VIP + freshness boosts) instead of re-applying them
            // here — re-applying double-counted VIP/freshness. Backend still owns the
            // final feed assembly via discovery-shift pinning below.
            RecommendationResponse response = buildResponse(result, "personalized", false, false);

            // Apply Discovery Shift slot pinning (Slots 8, 9, 10 for Discovery Zone)
            // isShift was pre-computed before the AI call.
            if (isShift && response.getListings() != null && response.getListings().size() >= 10) {
                List<ListingResponse> originalListings = response.getListings();
                List<ListingResponse> wardItems = new ArrayList<>();
                List<ListingResponse> districtItems = new ArrayList<>();
                List<ListingResponse> provinceItems = new ArrayList<>();
                List<ListingResponse> otherItems = new ArrayList<>();

                // Match discovery items against the Listing ENTITY using the SAME
                // representation that produced the discovery values (toFeatureDto):
                // new province/ward code + legacy district/ward id. Matching on
                // ListingResponse.address is wrong — that DTO holds only one
                // structure-dependent code (districtCode is null for new-structure
                // listings), so legacy-id-vs-code comparisons silently never match.
                Map<Long, Listing> entityById = new HashMap<>();
                for (Listing c : candidates) {
                    entityById.putIfAbsent(c.getListingId(), c);
                }
                Map<Long, Integer> discoveryLevelById = new HashMap<>();

                for (ListingResponse res : originalListings) {
                    int level = discoveryMatchLevel(entityById.get(res.getListingId()),
                            discoveryProvinceCode, discoveryDistrictId,
                            discoveryWardId, discoveryWardCode);
                    discoveryLevelById.put(res.getListingId(), level);
                    if (level == 3) {
                        wardItems.add(res);
                    } else if (level == 2) {
                        districtItems.add(res);
                    } else if (level == 1) {
                        provinceItems.add(res);
                    } else {
                        otherItems.add(res);
                    }
                }

                // Sort otherItems by preferred location match level descending to prioritize most viewed locations
                final Double fPrefLat = preferredLat;
                final Double fPrefLon = preferredLon;
                otherItems.sort((a, b) -> {
                    double levelA = preferredMatchLevel(entityById.get(a.getListingId()),
                            preferredProvinceCode, preferredDistrictId,
                            preferredWardId, preferredWardCode, fPrefLat, fPrefLon);
                    double levelB = preferredMatchLevel(entityById.get(b.getListingId()),
                            preferredProvinceCode, preferredDistrictId,
                            preferredWardId, preferredWardCode, fPrefLat, fPrefLon);
                    return Double.compare(levelB, levelA);
                });

                // Combine into discoveryItems with Priority: Ward > District > Province
                List<ListingResponse> rawDiscoveryItems = new ArrayList<>();
                rawDiscoveryItems.addAll(wardItems);
                rawDiscoveryItems.addAll(districtItems);
                rawDiscoveryItems.addAll(provinceItems);

                List<ListingResponse> discoveryItems = new ArrayList<>();
                Set<Long> seenDiscoveryIds = new HashSet<>();
                for (ListingResponse res : rawDiscoveryItems) {
                    if (seenDiscoveryIds.add(res.getListingId())) {
                        discoveryItems.add(res);
                    }
                }

                // HARD FALLBACK: If AI returned < 3 discovery items, fetch more directly from
                // the database!
                if (discoveryItems.size() < 3) {
                    List<Long> existingIds = new ArrayList<>(seenDiscoveryIds);
                    List<Long> queryExcludedIds = new ArrayList<>(interactedListingIds);
                    queryExcludedIds.addAll(existingIds);
                    if (queryExcludedIds.isEmpty())
                        queryExcludedIds.add(-1L);

                    List<Listing> backupListings = new ArrayList<>();

                    // 1. Try Ward
                    if (discoveryWardCode != null && !discoveryWardCode.isEmpty()) {
                        List<Listing> bWard = listingRepository.findCandidatesByNewWard(
                                discoveryWardCode, queryExcludedIds, PageRequest.of(0, 3));
                        backupListings.addAll(bWard);
                        bWard.forEach(b -> queryExcludedIds.add(b.getListingId()));
                    } else if (discoveryWardId != null) {
                        List<Listing> bWard = listingRepository.findCandidatesByLegacyWard(
                                discoveryWardId, queryExcludedIds, PageRequest.of(0, 3));
                        backupListings.addAll(bWard);
                        bWard.forEach(b -> queryExcludedIds.add(b.getListingId()));
                    }

                    // 2. Try District
                    if (backupListings.size() < 3 && discoveryDistrictId != null) {
                        List<Listing> bDistrict = listingRepository.findCandidatesByDistrict(
                                discoveryDistrictId, queryExcludedIds, PageRequest.of(0, 3 - backupListings.size()));
                        backupListings.addAll(bDistrict);
                        bDistrict.forEach(b -> queryExcludedIds.add(b.getListingId()));
                    }

                    // 3. Try Province
                    if (backupListings.size() < 3 && discoveryProvinceCode != null && !discoveryProvinceCode.isEmpty()
                            && !discoveryProvinceCode.equals("UNKNOWN")) {
                        List<Listing> bProvince = listingRepository.findCandidatesForPersonalizedByNewProvince(
                                discoveryProvinceCode, queryExcludedIds, PageRequest.of(0, 3 - backupListings.size()));
                        backupListings.addAll(bProvince);
                    } else if (backupListings.size() < 3 && discoveryProvinceCode != null) {
                        Integer pId = null;
                        try {
                            pId = Integer.parseInt(discoveryProvinceCode);
                        } catch (Exception ignored) {
                        }
                        if (pId != null) {
                            List<Listing> bProvince = listingRepository.findCandidatesForPersonalizedByLegacyProvince(
                                    pId, queryExcludedIds, PageRequest.of(0, 3 - backupListings.size()));
                            backupListings.addAll(bProvince);
                        }
                    }

                    if (!backupListings.isEmpty()) {
                        Set<Long> backupIds = backupListings.stream().map(Listing::getListingId)
                                .collect(Collectors.toSet());
                        List<ListingResponse> backupResponses = listingService.getListingsByIds(backupIds);

                        // Map responses back to DB retrieval order
                        Map<Long, ListingResponse> respMap = backupResponses.stream()
                                .collect(Collectors.toMap(ListingResponse::getListingId, item -> item));

                        for (Listing l : backupListings) {
                            if (discoveryItems.size() >= 3)
                                break;
                            ListingResponse br = respMap.get(l.getListingId());
                            if (br != null && seenDiscoveryIds.add(br.getListingId())) {
                                discoveryItems.add(br);
                                discoveryLevelById.put(br.getListingId(), discoveryMatchLevel(
                                        l, discoveryProvinceCode, discoveryDistrictId,
                                        discoveryWardId, discoveryWardCode));
                            }
                        }
                    }
                }

                // Re-sort discoveryItems by strict hierarchical priority (AI + Backup combined)
                if (!discoveryItems.isEmpty()) {
                    List<ListingResponse> finalWardItems = new ArrayList<>();
                    List<ListingResponse> finalDistrictItems = new ArrayList<>();
                    List<ListingResponse> finalProvinceItems = new ArrayList<>();

                    for (ListingResponse res : discoveryItems) {
                        int level = discoveryLevelById.getOrDefault(res.getListingId(), 1);
                        if (level == 3) {
                            finalWardItems.add(res);
                        } else if (level == 2) {
                            finalDistrictItems.add(res);
                        } else {
                            finalProvinceItems.add(res);
                        }
                    }

                    discoveryItems.clear();
                    discoveryItems.addAll(finalWardItems);
                    discoveryItems.addAll(finalDistrictItems);
                    discoveryItems.addAll(finalProvinceItems);
                }

                // HARD FALLBACK FOR PREFERRED DISTRICT: If AI returned < 7 items at
                // district-level or close distance (preferredMatchLevel >= 1.25, i.e., within 3km)
                // for slots 1-7, fetch more from the preferred district directly from DB.
                if (preferredDistrictId != null) {
                    long prefDistrictCount = otherItems.stream()
                            .limit(7)
                            .filter(r -> preferredMatchLevel(entityById.get(r.getListingId()),
                                    preferredProvinceCode, preferredDistrictId,
                                    preferredWardId, preferredWardCode, fPrefLat, fPrefLon) >= 1.25)
                            .count();

                    if (prefDistrictCount < 7) {
                        int needed = (int) (7 - prefDistrictCount);
                        List<Long> prefExclusionIds = new ArrayList<>(interactedListingIds);
                        otherItems.forEach(r -> prefExclusionIds.add(r.getListingId()));
                        discoveryItems.forEach(r -> prefExclusionIds.add(r.getListingId()));
                        if (prefExclusionIds.isEmpty()) prefExclusionIds.add(-1L);

                        List<Listing> morePref = new ArrayList<>(
                                listingRepository.findCandidatesByDistrict(
                                        preferredDistrictId, prefExclusionIds,
                                        PageRequest.of(0, needed)));

                        // Fallback to preferred ward if district still insufficient
                        if (morePref.isEmpty() && preferredWardCode != null
                                && !preferredWardCode.isEmpty()) {
                            morePref.addAll(listingRepository.findCandidatesByNewWard(
                                    preferredWardCode, prefExclusionIds,
                                    PageRequest.of(0, needed)));
                        } else if (morePref.isEmpty() && preferredWardId != null) {
                            morePref.addAll(listingRepository.findCandidatesByLegacyWard(
                                    preferredWardId, prefExclusionIds,
                                    PageRequest.of(0, needed)));
                        }

                        if (!morePref.isEmpty()) {
                            Set<Long> morePrefIds = morePref.stream()
                                    .map(Listing::getListingId).collect(Collectors.toSet());
                            List<ListingResponse> morePrefResponses =
                                    listingService.getListingsByIds(morePrefIds);
                            Map<Long, ListingResponse> morePrefMap = morePrefResponses.stream()
                                    .collect(Collectors.toMap(
                                            ListingResponse::getListingId, r -> r));

                            Set<Long> prefSeenIds = new HashSet<>();
                            otherItems.forEach(r -> prefSeenIds.add(r.getListingId()));

                            for (Listing l : morePref) {
                                ListingResponse r = morePrefMap.get(l.getListingId());
                                if (r != null && prefSeenIds.add(r.getListingId())) {
                                    entityById.putIfAbsent(l.getListingId(), l);
                                    otherItems.add(r);
                                }
                            }
                            // Re-sort after appending new preferred-district items
                            final String fppc = preferredProvinceCode;
                            final Integer fpdi = preferredDistrictId;
                            final Integer fpwi = preferredWardId;
                            final String fpwc = preferredWardCode;
                            otherItems.sort((a, b) -> Double.compare(
                                    preferredMatchLevel(entityById.get(b.getListingId()),
                                            fppc, fpdi, fpwi, fpwc, fPrefLat, fPrefLon),
                                    preferredMatchLevel(entityById.get(a.getListingId()),
                                            fppc, fpdi, fpwi, fpwc, fPrefLat, fPrefLon)));
                            log.info("[Recommendation] Preferred-district hard fallback for user {}: "
                                    + "added {} items from district {}", userId, morePref.size(), preferredDistrictId);
                        }
                    }
                }

                // If we have discovery items, let's pin exactly 3 to slots 8, 9, 10
                if (!discoveryItems.isEmpty()) {
                    List<ListingResponse> pinnedListings = new ArrayList<>();
                    Set<Long> finalSeenIds = new HashSet<>();

                    // 1. Take the first 7 non-discovery items (Slots 1 to 7)
                    int otherToTake = Math.min(7, otherItems.size());
                    for (int i = 0; i < otherToTake; i++) {
                        ListingResponse item = otherItems.get(i);
                        if (finalSeenIds.add(item.getListingId())) {
                            pinnedListings.add(item);
                        }
                    }

                    // 2. Take up to 3 discovery items (Slots 8, 9, 10)
                    int discoveryToTake = Math.min(3, discoveryItems.size());
                    for (int i = 0; i < discoveryToTake; i++) {
                        ListingResponse item = discoveryItems.get(i);
                        if (finalSeenIds.add(item.getListingId())) {
                            pinnedListings.add(item);
                        }
                    }

                    // 3. Top-up with remaining items
                    for (int i = otherToTake; i < otherItems.size(); i++) {
                        ListingResponse item = otherItems.get(i);
                        if (finalSeenIds.add(item.getListingId())) {
                            pinnedListings.add(item);
                        }
                    }
                    for (int i = discoveryToTake; i < discoveryItems.size(); i++) {
                        ListingResponse item = discoveryItems.get(i);
                        if (finalSeenIds.add(item.getListingId())) {
                            pinnedListings.add(item);
                        }
                    }

                    // Keep the feed at exactly topN. The hard-fallback backup can add
                    // discovery items that were NOT in the AI's original topN, which
                    // would otherwise grow the list past topN. The 3 pinned discovery
                    // items live at slots 8/9/10 (indices 7-9 < topN, since pinning
                    // only runs when the feed already has >= 10 items), so trimming
                    // the tail drops the lowest-ranked overflow, never the pins.
                    if (pinnedListings.size() > topN) {
                        pinnedListings = new ArrayList<>(pinnedListings.subList(0, topN));
                    }

                    response.setListings(pinnedListings);
                    response.setTotalReturned(pinnedListings.size());
                    log.info(
                            "[Recommendation] Applied Discovery Shift Pinning for user {}: pinned {} items, final size {}",
                            userId, discoveryToTake, pinnedListings.size());
                }
            }

            return response;
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
        if (type == null)
            return 0.2;
        return switch (type.toUpperCase()) {
            case "DIAMOND" -> 1.0;
            case "GOLD" -> 0.7;
            case "SILVER" -> 0.4;
            default -> 0.2;
        };
    }

    private double freshnessScore(ListingResponse res) {
        LocalDateTime date = res.getPushedAt() != null ? res.getPushedAt() : res.getPostDate();
        if (date == null)
            return 0.0;
        long days = ChronoUnit.DAYS.between(date, LocalDateTime.now());
        return 1.0 / (1.0 + Math.max(0, days));
    }

    /**
     * Discovery-zone match precision for a candidate, computed on the Listing
     * ENTITY via the same toFeatureDto representation used to derive the discovery
     * values (new province/ward code + legacy district/ward id). Comparing like
     * for like avoids the structure-dependent code mismatch in ListingResponse.
     *
     * @return 3 = ward, 2 = district, 1 = province, 0 = outside the discovery zone.
     */
    private int discoveryMatchLevel(Listing listing, String discoveryProvinceCode,
            Integer discoveryDistrictId, Integer discoveryWardId, String discoveryWardCode) {
        if (listing == null) {
            return 0;
        }
        AIRecommendationRequest.ListingFeatureDto f = toFeatureDto(listing);
        if ((discoveryWardCode != null && discoveryWardCode.equals(f.getNewWardCode()))
                || (discoveryWardId != null && discoveryWardId.equals(f.getWardId()))) {
            return 3;
        }
        if (discoveryDistrictId != null && discoveryDistrictId.equals(f.getDistrictId())) {
            return 2;
        }
        if (discoveryProvinceCode != null && discoveryProvinceCode.equals(f.getProvinceCode())) {
            return 1;
        }
        return 0;
    }

    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }
        double earthRadius = 6371; // kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private double preferredMatchLevel(Listing listing, String preferredProvinceCode,
            Integer preferredDistrictId, Integer preferredWardId, String preferredWardCode,
            Double preferredLat, Double preferredLon) {
        if (listing == null) {
            return 0.0;
        }
        AIRecommendationRequest.ListingFeatureDto f = toFeatureDto(listing);
        if ((preferredWardCode != null && preferredWardCode.equals(f.getNewWardCode()))
                || (preferredWardId != null && preferredWardId.equals(f.getWardId()))) {
            return 3.0;
        }
        if (preferredDistrictId != null && preferredDistrictId.equals(f.getDistrictId())) {
            return 2.0;
        }
        if (preferredProvinceCode != null && preferredProvinceCode.equals(f.getProvinceCode())) {
            if (preferredLat != null && preferredLat > 0 && preferredLon != null && preferredLon > 0
                    && f.getLatitude() != null && f.getLatitude() > 0
                    && f.getLongitude() != null && f.getLongitude() > 0) {
                double distance = calculateDistance(preferredLat, preferredLon, f.getLatitude(), f.getLongitude());
                return 1.0 + (1.0 / (1.0 + distance));
            }
            return 1.0;
        }
        return 0.0;
    }

    /**
     * Whether an interacted listing sits in a DIFFERENT place than the user's
     * preferred location.
     *
     * Discovery Shift is a province-level concept: we only count a listing as
     * "different from preferred" when it is in a DIFFERENT PROVINCE. Within-province
     * browsing (different ward / district in the same city) is normal behaviour and
     * must NOT inflate diffLocViews or trigger shift.
     *
     * A null feature (listing not resolvable) does not count as different, to
     * avoid over-triggering the shift.
     */
    private boolean isDifferentFromPreferred(AIRecommendationRequest.ListingFeatureDto f,
            String preferredProvinceCode, Integer preferredDistrictId,
            Integer preferredWardId, String preferredWardCode) {
        if (f == null) {
            return false;
        }
        if (preferredProvinceCode != null && !preferredProvinceCode.equals(f.getProvinceCode())) {
            return true;
        }
        if (preferredDistrictId != null && !preferredDistrictId.equals(f.getDistrictId())) {
            return true;
        }
        if (preferredWardCode != null && !preferredWardCode.equals(f.getNewWardCode())) {
            return true;
        }
        if (preferredWardId != null && !preferredWardId.equals(f.getWardId())) {
            return true;
        }
        return false;
    }

    private AIRecommendationRequest.ListingFeatureDto toFeatureDto(Listing listing) {
        int daysAgo = 0;
        LocalDateTime date = listing.getPushedAt() != null ? listing.getPushedAt() : listing.getPostDate();
        if (date != null)
            daysAgo = (int) ChronoUnit.DAYS.between(date, LocalDateTime.now());

        String pCode = "UNKNOWN";
        String wCode = null;
        Integer dId = null;
        Integer wId = null;

        Double latitude = null;
        Double longitude = null;

        if (listing.getAddress() != null) {
            var addr = listing.getAddress();
            pCode = addr.getNewProvinceCode();
            wCode = addr.getNewWardCode();
            dId = addr.getLegacyDistrictId();
            wId = addr.getLegacyWardId();
            latitude = addr.getLatitude() != null ? addr.getLatitude().doubleValue() : null;
            longitude = addr.getLongitude() != null ? addr.getLongitude().doubleValue() : null;

            // Fallback: If new codes are missing, try to resolve from mapping
            if (pCode == null || pCode.isEmpty()) {
                if (addr.getLegacyProvinceId() != null && dId != null && wId != null) {
                    String mapProv = String.format("%02d", addr.getLegacyProvinceId());
                    String mapDist = String.format("%03d", dId);
                    String mapWard = String.format("%05d", wId);
                    var mappingOpt = legacyMappingCache.computeIfAbsent(
                            mapProv + "|" + mapDist + "|" + mapWard,
                            k -> addressMappingRepository.findBestByLegacyAddress(mapProv, mapDist, mapWard));
                    if (mappingOpt.isPresent()) {
                        pCode = mappingOpt.get().getNewProvinceCode();
                        wCode = mappingOpt.get().getNewWardCode();
                    }
                }
                // If still missing, fallback to legacy province ID
                if (pCode == null || pCode.isEmpty()) {
                    if (addr.getLegacyProvinceId() != null) {
                        pCode = String.valueOf(addr.getLegacyProvinceId());
                    } else {
                        pCode = "UNKNOWN";
                    }
                }
            }
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
                .wardId(wId)
                .newWardCode(wCode)
                .vipType(listing.getVipType() != null ? listing.getVipType().name() : "NORMAL")
                .postDateDaysAgo(Math.max(0, daysAgo))
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    @lombok.AllArgsConstructor
    private static class ScoredListing {
        ListingResponse listing;
        double score;
    }
}
