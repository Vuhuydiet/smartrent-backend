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
        Map<Long, Double> interactionWeightMap = new LinkedHashMap<>();
        List<SavedListing> savedListings = savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Long> clickedListingIds = phoneClickDetailRepository.findListingIdsByUserId(userId);
        List<RecentlyViewedItemResponse> viewedListings = recentlyViewedService.getRecentlyViewed(userId);

        for (SavedListing s : savedListings) {
            interactionWeightMap.merge(s.getId().getListingId(), 3.0, Math::max);
        }
        for (Long lId : clickedListingIds) {
            interactionWeightMap.merge(lId, 2.5, Math::max);
        }
        for (RecentlyViewedItemResponse rv : viewedListings) {
            interactionWeightMap.merge(rv.getListing().getListingId(), 1.0, Math::max);
        }

        boolean hasEnoughInteractions = !savedListings.isEmpty() || !clickedListingIds.isEmpty() || viewedListings.size() >= 3;
        if (!hasEnoughInteractions) {
            return getColdStartFeed(topN);
        }


        List<AIRecommendationRequest.InteractionEntryDto> userInteractions = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : interactionWeightMap.entrySet()) {
            userInteractions.add(new AIRecommendationRequest.InteractionEntryDto(userId, entry.getKey(), entry.getValue()));
        }

        // 2. Feature extraction for user's interacted listings (for profile building)
        Set<Long> interactedListingIds = interactionWeightMap.keySet();
        List<Listing> interactedListings = listingRepository.findWithAddressByListingIds(interactedListingIds);

        // Sort interactedListings so that the most recently viewed items come FIRST
        List<Long> recencyOrder = recentlyViewedService.getRecentlyViewed(userId).stream()
                .map(rv -> rv.getListing().getListingId())
                .collect(Collectors.toList());
        
        interactedListings.sort((l1, l2) -> {
            int idx1 = recencyOrder.indexOf(l1.getListingId());
            int idx2 = recencyOrder.indexOf(l2.getListingId());
            // If not in recent views, treat as older (push to back)
            if (idx1 == -1) idx1 = Integer.MAX_VALUE;
            if (idx2 == -1) idx2 = Integer.MAX_VALUE;
            return Integer.compare(idx1, idx2);
        });

        List<AIRecommendationRequest.ListingFeatureDto> interactionFeatures = interactedListings.stream()
                .map(this::toFeatureDto).collect(Collectors.toList());

        // 3. Location profiling
        // 3.1 Find most frequent (preferred) locations
        String preferredProvinceCode = interactionFeatures.stream()
                .filter(f -> f.getProvinceCode() != null && !f.getProvinceCode().equals("UNKNOWN"))
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getProvinceCode, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        String preferredWardCode = interactionFeatures.stream()
                .filter(f -> f.getNewWardCode() != null)
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getNewWardCode, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Integer preferredDistrictId = interactionFeatures.stream()
                .filter(f -> f.getDistrictId() != null)
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getDistrictId, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Integer preferredWardId = interactionFeatures.stream()
                .filter(f -> f.getWardId() != null)
                .collect(Collectors.groupingBy(AIRecommendationRequest.ListingFeatureDto::getWardId, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        // 3.2 Find latest interacted location (Discovery Zone)
        // 3.2 Find latest interacted location (Discovery Zone)
        Long latestListingId = null;
        if (!viewedListings.isEmpty()) {
            latestListingId = viewedListings.get(0).getListing().getListingId();
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
        log.info("[Recommendation Test] latestListingId = {}, discoveryProvinceCode = {}, preferredProvinceCode = {}", latestListingId, discoveryProvinceCode, preferredProvinceCode);



        // 4. Hierarchical candidate pool (4-tier strategy)
        List<Listing> candidates = new ArrayList<>();
        Set<Long> excludedIds = new HashSet<>(interactedListingIds);
        if (excludedIds.isEmpty()) excludedIds.add(-1L); // Prevent empty list error in SQL IN clause

        // Tier 1: Same Ward (50 items)
        if (preferredWardCode != null || preferredWardId != null) {
            List<Listing> t1 = listingRepository.findCandidatesByWard(
                    preferredWardId, preferredWardCode, new ArrayList<>(excludedIds), PageRequest.of(0, 50));
            candidates.addAll(t1);
            t1.forEach(c -> excludedIds.add(c.getListingId()));
        }

        // Tier 2: Same District (100 items)
        if (preferredDistrictId != null) {
            List<Listing> t2 = listingRepository.findCandidatesByDistrict(
                    preferredDistrictId, new ArrayList<>(excludedIds), PageRequest.of(0, 100));
            candidates.addAll(t2);
            t2.forEach(c -> excludedIds.add(c.getListingId()));
        }

        // Tier 3: Discovery Zone (up to 50 items from latest location - cascading Ward -> District -> Province)
        boolean isShift = false;
        if (discoveryProvinceCode != null && preferredProvinceCode != null && !discoveryProvinceCode.equals(preferredProvinceCode)) {
            isShift = true;
        } else if (discoveryDistrictId != null && preferredDistrictId != null && !discoveryDistrictId.equals(preferredDistrictId)) {
            isShift = true;
        } else if (discoveryWardCode != null && preferredWardCode != null && !discoveryWardCode.equals(preferredWardCode)) {
            isShift = true;
        } else if (discoveryWardId != null && preferredWardId != null && !discoveryWardId.equals(preferredWardId)) {
            isShift = true;
        }        log.info("[Recommendation Test] isShift evaluated to: {}", isShift);

        if (isShift) {

            
            List<Listing> t3 = new ArrayList<>();
            
            // 1. Try Ward
            if (discoveryWardCode != null || discoveryWardId != null) {
                t3 = listingRepository.findCandidatesByWard(
                        discoveryWardId, discoveryWardCode, new ArrayList<>(excludedIds), PageRequest.of(0, 50));
            }
            
            // 2. Try District (if not enough)
            if (t3.size() < 10 && discoveryDistrictId != null) {
                List<Listing> t3District = listingRepository.findCandidatesByDistrict(
                        discoveryDistrictId, new ArrayList<>(excludedIds), PageRequest.of(0, 50 - t3.size()));
                t3.addAll(t3District);
            }
            
            // 3. Try Province (if still not enough)
            if (t3.size() < 10 && discoveryProvinceCode != null) {
                List<Listing> t3Province = listingRepository.findCandidatesForPersonalized(
                        null, discoveryProvinceCode, new ArrayList<>(excludedIds), PageRequest.of(0, 50 - t3.size()));
                t3.addAll(t3Province);
            }
            
            candidates.addAll(t3);
            t3.forEach(c -> excludedIds.add(c.getListingId()));
            log.info("[Recommendation] User {} → added {} discovery candidates cascading from ward/district/province", userId, t3.size());
        }


        // Tier 4: Same Province (up to 300 total)
        if (preferredProvinceCode != null) {
            Integer pId = null;
            try { pId = Integer.parseInt(preferredProvinceCode); } catch (Exception ignored) {}
            int remaining = Math.max(0, 300 - candidates.size());
            if (remaining > 0) {
                List<Listing> t4 = listingRepository.findCandidatesForPersonalized(
                        pId, preferredProvinceCode, new ArrayList<>(excludedIds), PageRequest.of(0, remaining));
                candidates.addAll(t4);
                t4.forEach(c -> excludedIds.add(c.getListingId()));
            }
        }

        // Fallback global top-up if still less than 150
        if (candidates.size() < 150) {
            int globalSize = 300 - candidates.size();
            candidates.addAll(listingRepository.findCandidatesForPersonalizedGlobal(
                    new ArrayList<>(excludedIds), PageRequest.of(0, globalSize)));
        }

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

        AIRecommendationRequest.PersonalizedFeedAiRequest aiRequest =
                AIRecommendationRequest.PersonalizedFeedAiRequest.builder()
                        .user_id(userId)
                        .user_interactions(userInteractions)
                        .all_interactions(allInteractions)
                        .candidates(distinctCandidates)
                        .top_n(topN)
                        .alpha(0.5)
                        .interactionFeatures(interactionFeatures)
                        .build();

        try {
            List<RecommendationItemDto> result = aiConnector.getPersonalizedFeed(aiRequest);
            RecommendationResponse response = buildResponse(result, "personalized", false, true);

            // Apply Discovery Shift slot pinning (Slots 8, 9, 10 for Discovery Zone)
            isShift = false;
            if (discoveryProvinceCode != null && preferredProvinceCode != null && !discoveryProvinceCode.equals(preferredProvinceCode)) {
                isShift = true;
            } else if (discoveryDistrictId != null && preferredDistrictId != null && !discoveryDistrictId.equals(preferredDistrictId)) {
                isShift = true;
            } else if (discoveryWardCode != null && preferredWardCode != null && !discoveryWardCode.equals(preferredWardCode)) {
                isShift = true;
            } else if (discoveryWardId != null && preferredWardId != null && !discoveryWardId.equals(preferredWardId)) {
                isShift = true;
            }


            if (isShift && response.getListings() != null && response.getListings().size() >= 10) {
                List<ListingResponse> originalListings = response.getListings();
                List<ListingResponse> wardItems = new ArrayList<>();
                List<ListingResponse> districtItems = new ArrayList<>();
                List<ListingResponse> provinceItems = new ArrayList<>();
                List<ListingResponse> otherItems = new ArrayList<>();

                for (ListingResponse res : originalListings) {
                    if (res.getAddress() != null) {
                        String itemWardCode = res.getAddress().getWardCode();
                        String itemDistrictCode = res.getAddress().getDistrictCode();
                        String itemProvinceCode = res.getAddress().getProvinceCode();

                        boolean isWardMatch = (discoveryWardCode != null && discoveryWardCode.equals(itemWardCode)) ||
                                            (discoveryWardId != null && String.valueOf(discoveryWardId).equals(itemWardCode));
                        
                        boolean isDistrictMatch = false;
                        if (!isWardMatch && discoveryDistrictId != null && itemDistrictCode != null) {
                            if (String.valueOf(discoveryDistrictId).equals(itemDistrictCode)) {
                                isDistrictMatch = true;
                            }
                        }

                        boolean isProvinceMatch = false;
                        if (!isWardMatch && !isDistrictMatch && discoveryProvinceCode != null && discoveryProvinceCode.equals(itemProvinceCode)) {
                            isProvinceMatch = true;
                        }

                        if (isWardMatch) {
                            wardItems.add(res);
                        } else if (isDistrictMatch) {
                            districtItems.add(res);
                        } else if (isProvinceMatch) {
                            provinceItems.add(res);
                        } else {
                            otherItems.add(res);
                        }
                    } else {
                        otherItems.add(res);
                    }
                }

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

                // HARD FALLBACK: If AI returned < 3 discovery items, fetch more directly from the database!
                if (discoveryItems.size() < 3) {
                    List<Long> existingIds = new ArrayList<>(seenDiscoveryIds);
                    List<Long> queryExcludedIds = new ArrayList<>(interactedListingIds);
                    queryExcludedIds.addAll(existingIds);
                    if (queryExcludedIds.isEmpty()) queryExcludedIds.add(-1L);
                    
                    List<Listing> backupListings = new ArrayList<>();
                    
                    // 1. Try Ward
                    if (discoveryWardCode != null || discoveryWardId != null) {
                        List<Listing> bWard = listingRepository.findCandidatesByWard(
                                discoveryWardId, discoveryWardCode, queryExcludedIds, PageRequest.of(0, 3));
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
                    if (backupListings.size() < 3 && discoveryProvinceCode != null) {
                        Integer pId = null;
                        try { pId = Integer.parseInt(discoveryProvinceCode); } catch (Exception ignored) {}
                        List<Listing> bProvince = listingRepository.findCandidatesForPersonalized(
                                pId, discoveryProvinceCode, queryExcludedIds, PageRequest.of(0, 3 - backupListings.size()));
                        backupListings.addAll(bProvince);
                    }
                            
                    if (!backupListings.isEmpty()) {
                        Set<Long> backupIds = backupListings.stream().map(Listing::getListingId).collect(Collectors.toSet());
                        List<ListingResponse> backupResponses = listingService.getListingsByIds(backupIds);
                        
                        // Map responses back to DB retrieval order
                        Map<Long, ListingResponse> respMap = backupResponses.stream()
                                .collect(Collectors.toMap(ListingResponse::getListingId, item -> item));
                        
                        for (Listing l : backupListings) {
                            if (discoveryItems.size() >= 3) break;
                            ListingResponse br = respMap.get(l.getListingId());
                            if (br != null && seenDiscoveryIds.add(br.getListingId())) {
                                discoveryItems.add(br);
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
                        if (res.getAddress() != null) {
                            String itemWardCode = res.getAddress().getWardCode();
                            String itemDistrictCode = res.getAddress().getDistrictCode();
                            
                            boolean isWardMatch = (discoveryWardCode != null && discoveryWardCode.equals(itemWardCode)) ||
                                                 (discoveryWardId != null && String.valueOf(discoveryWardId).equals(itemWardCode));
                            
                            boolean isDistrictMatch = false;
                            if (!isWardMatch && discoveryDistrictId != null && itemDistrictCode != null) {
                                if (String.valueOf(discoveryDistrictId).equals(itemDistrictCode)) {
                                    isDistrictMatch = true;
                                }
                            }
                            
                            if (isWardMatch) {
                                finalWardItems.add(res);
                            } else if (isDistrictMatch) {
                                finalDistrictItems.add(res);
                            } else {
                                finalProvinceItems.add(res);
                            }
                        } else {
                            finalProvinceItems.add(res);
                        }
                    }
                    
                    discoveryItems.clear();
                    discoveryItems.addAll(finalWardItems);
                    discoveryItems.addAll(finalDistrictItems);
                    discoveryItems.addAll(finalProvinceItems);
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
                    
                    response.setListings(pinnedListings);
                    response.setTotalReturned(pinnedListings.size());
                    log.info("[Recommendation] Applied Discovery Shift Pinning for user {}: pinned {} items, final size {}", userId, discoveryToTake, pinnedListings.size());
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
        String wCode = null;
        Integer dId = null;
        Integer wId = null;

        if (listing.getAddress() != null) {
            var addr = listing.getAddress();
            pCode = addr.getNewProvinceCode();
            wCode = addr.getNewWardCode();
            dId = addr.getLegacyDistrictId();
            wId = addr.getLegacyWardId();

            // Fallback: If new codes are missing, try to resolve from mapping
            if (pCode == null || pCode.isEmpty()) {
                if (addr.getLegacyProvinceId() != null && dId != null && wId != null) {
                    var mappingOpt = addressMappingRepository.findBestByLegacyAddress(
                            String.format("%02d", addr.getLegacyProvinceId()),
                            String.format("%03d", dId),
                            String.format("%05d", wId));
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
                .build();
    }

    @lombok.AllArgsConstructor
    private static class ScoredListing {
        ListingResponse listing;
        double score;
    }
}
