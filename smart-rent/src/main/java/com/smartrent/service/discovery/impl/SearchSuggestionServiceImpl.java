package com.smartrent.service.discovery.impl;

import com.smartrent.dto.response.SearchSuggestionItem;
import com.smartrent.dto.response.SearchSuggestionsResponse;
import com.smartrent.enums.SuggestionType;
import com.smartrent.infra.repository.LegacyProvinceRepository;
import com.smartrent.infra.repository.LegacyWardRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.SearchQueryImpressionRepository;
import com.smartrent.infra.repository.SearchSuggestionClickRepository;
import com.smartrent.infra.repository.entity.LegacyProvince;
import com.smartrent.infra.repository.entity.LegacyWard;
import com.smartrent.infra.repository.entity.SearchQueryImpression;
import com.smartrent.infra.repository.entity.SearchSuggestionClick;
import com.smartrent.service.discovery.SearchSuggestionService;
import com.smartrent.util.TextNormalizer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link SearchSuggestionService}.
 *
 * <h3>Suggestion sources and weights</h3>
 * <ul>
 *   <li>TITLE        — 1.5 (listing title full-text match; highest confidence)</li>
 *   <li>LOCATION     — 1.0 (province / district / ward name prefix match)</li>
 *   <li>POPULAR_QUERY— 0.8 (historically clicked terms)</li>
 * </ul>
 *
 * <h3>Score formula</h3>
 * {@code score = baseWeight * (1 - 0.05 * zeroIndexedRank)}
 * This gently decays score with position so that VIP-boosted titles at the top
 * still outrank later matches.
 *
 * <h3>Telemetry writes</h3>
 * Impression persists are executed in a separate {@code REQUIRES_NEW} transaction
 * so that a caching hit (which bypasses the main method body) does NOT prevent
 * the impression from being recorded.  The click persist follows the same pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchSuggestionServiceImpl implements SearchSuggestionService {

    // ── Weights ──────────────────────────────────────────────────────────────
    private static final double WEIGHT_TITLE         = 1.5;
    private static final double WEIGHT_LOCATION      = 1.0;
    private static final double WEIGHT_POPULAR_QUERY = 0.8;
    private static final double SCORE_DECAY_PER_RANK = 0.05;

    /** Maximum number of location suggestions fetched per ward search. */
    private static final int MAX_LOCATION_CANDIDATES = 8;

    /** Rolling window for popular-query aggregation (7 days). */
    private static final long POPULAR_QUERY_WINDOW_DAYS = 7;

    /**
     * Province codes returned as default LOCATION suggestions when the user
     * focuses the search box without typing yet. Order is intentional and
     * preserved in the response (TP. HCM first by convention).
     */
    private static final List<String> DEFAULT_TOP_PROVINCE_CODES = List.of(
            "79", // TP. Hồ Chí Minh
            "01", // Hà Nội
            "48", // Đà Nẵng
            "75"  // Đồng Nai
    );

    // ── Dependencies ─────────────────────────────────────────────────────────
    ListingRepository             listingRepository;
    LegacyWardRepository          legacyWardRepository;
    LegacyProvinceRepository      legacyProvinceRepository;
    SearchQueryImpressionRepository impressionRepository;
    SearchSuggestionClickRepository clickRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Main entry point — results are cached with a short TTL (see {@code listing.suggestions}
     * cache definition in {@code application.yml}).
     *
     * <p>The cache key encodes: normalized query + provinceId + categoryId + limit.
     * Telemetry (impression persist) is intentionally written <em>outside</em> the
     * cached path to ensure every unique user call is counted even if the suggestion
     * list is served from cache.
     */
    @Override
    @Cacheable(
        cacheNames = com.smartrent.config.Constants.CacheNames.LISTING_SUGGESTIONS,
        key         = "T(com.smartrent.util.CacheKeyBuilder).suggestionKey(#query, #provinceId, #categoryId, #limit)",
        unless      = "#result == null || #result.suggestions.isEmpty()"
    )
    @Transactional(readOnly = true)
    public SearchSuggestionsResponse getSuggestions(
            String query,
            int    limit,
            String provinceId,
            Long   categoryId,
            String clientIp,
            String sessionId) {

        int safeLimit = Math.min(Math.max(limit, 1), 20);

        String normalized = TextNormalizer.normalize(query);
        if (normalized == null || normalized.length() < 2) {
            // Empty / very short query → return curated top-city LOCATION
            // suggestions so the dropdown is useful on focus before typing.
            log.debug("search-suggestions: query too short, returning default top-city suggestions");
            List<SearchSuggestionItem> defaults = fetchDefaultLocationSuggestions(safeLimit);
            long impressionId = persistImpression(
                    Objects.toString(normalized, ""), provinceId, categoryId,
                    defaults.size(), clientIp, sessionId);
            return SearchSuggestionsResponse.builder()
                    .suggestions(defaults)
                    .queryNorm(Objects.toString(normalized, ""))
                    .impressionId(impressionId)
                    .build();
        }

        Integer provinceIdInt = parseProvinceId(provinceId);

        // ── Fetch candidates from all three sources ──────────────────────────
        List<SearchSuggestionItem> titleItems    = fetchTitleSuggestions(normalized, provinceIdInt, categoryId, safeLimit);
        List<SearchSuggestionItem> locationItems = fetchLocationSuggestions(normalized, provinceId);
        List<SearchSuggestionItem> popularItems  = fetchPopularQuerySuggestions(normalized, safeLimit);

        // ── Merge, deduplicate, rank, and trim ───────────────────────────────
        List<SearchSuggestionItem> merged = mergeAndRank(titleItems, locationItems, popularItems, safeLimit);

        // ── Persist impression (separate TX — does not affect cache result) ──
        long impressionId = persistImpression(normalized, provinceId, categoryId, merged.size(), clientIp, sessionId);

        log.debug("search-suggestions: q='{}' → {} suggestions (impression {})", normalized, merged.size(), impressionId);

        return SearchSuggestionsResponse.builder()
                .suggestions(merged)
                .queryNorm(normalized)
                .impressionId(impressionId)
                .build();
    }

    /**
     * Records a user click in a new transaction independent of the caller's transaction.
     * Swallows all exceptions so a telemetry write failure never propagates to the API caller.
     */
    @Override
    public void recordClick(long impressionId, SuggestionType type, String text, Long listingId, int rank) {
        try {
            persistClick(impressionId, type, text, listingId, rank);
        } catch (Exception e) {
            log.warn("search-suggestions: click telemetry write failed (non-fatal): {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — Suggestion fetchers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches TITLE suggestions from the listings table via a lightweight native projection.
     * Fetches up to {@code limit} rows; the final merged list is trimmed later.
     */
    private List<SearchSuggestionItem> fetchTitleSuggestions(
            String normalized, Integer provinceIdInt, Long categoryId, int limit) {
        try {
            String fulltextQuery = toBooleanFulltextQuery(normalized);
            if (fulltextQuery == null) {
                return Collections.emptyList();
            }

            List<Object[]> rows = listingRepository.findTitleSuggestions(
                    fulltextQuery, provinceIdInt, categoryId, limit);

            List<SearchSuggestionItem> items = new ArrayList<>(rows.size());
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                Long   listingId = toLong(row[0]);
                String title     = toString(row[1]);
                String addr      = chooseAddress(toString(row[2]), toString(row[3]));

                Map<String, Object> meta = new HashMap<>();
                if (addr != null) meta.put("address", addr);

                double score = WEIGHT_TITLE * (1.0 - SCORE_DECAY_PER_RANK * i);

                items.add(SearchSuggestionItem.builder()
                        .type(SuggestionType.TITLE)
                        .text(title)
                        .listingId(listingId)
                        .metadata(meta)
                        .score(score)
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("search-suggestions: title fetch failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Converts normalized input into a MySQL boolean-mode query.
     * Uses AND semantics across indexed terms and keeps prefix matching within each term,
     * so "ho" no longer has to be the beginning of the whole title to match "can ho".
     *
     * <p>Words with fewer than 2 chars are skipped (below MySQL's default
     * {@code ft_min_word_len = 2}); a single 2-char word is still emitted as a
     * prefix so that early keystrokes ("ho", "ch") still produce title matches
     * instead of an empty TITLE source. If every word is dropped, returns null
     * and the caller falls back to LOCATION / POPULAR_QUERY suggestions.
     */
    private String toBooleanFulltextQuery(String normalized) {
        if (normalized == null || normalized.isBlank()) return null;

        StringBuilder query = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            if (word.length() < 2) continue;
            if (!query.isEmpty()) query.append(' ');
            query.append('+').append(word).append('*');
        }

        return query.isEmpty() ? null : query.toString();
    }

    /**
     * Fetches LOCATION suggestions from the {@code legacy_wards} table.
     * Matches on the ward's {@code key} field (already normalized, e.g. "ba dinh")
     * so Vietnamese input works correctly via TextNormalizer parity.
     *
     * <p>Also matches province/district names to provide coarser-grained location suggestions.
     * Results are deduplicated by {@code wardName + districtName + provinceName} combination.
     */
    private List<SearchSuggestionItem> fetchLocationSuggestions(String normalized, String rawProvinceId) {
        try {
            List<LegacyWard> wards;
            PageRequest page = PageRequest.of(0, MAX_LOCATION_CANDIDATES);
            if (rawProvinceId != null && !rawProvinceId.isBlank()) {
                wards = legacyWardRepository.findSuggestionCandidatesByProvince(
                        toProvinceCode(rawProvinceId), normalized, page);
            } else {
                wards = legacyWardRepository.findSuggestionCandidates(normalized, page);
            }

            List<SearchSuggestionItem> items = new ArrayList<>();

            for (LegacyWard ward : wards) {
                if (items.size() >= MAX_LOCATION_CANDIDATES) break;

                boolean matchesWard     = ward.getKey() != null && ward.getKey().contains(normalized);
                boolean matchesDistrict = ward.getDistrictKey() != null && ward.getDistrictKey().contains(normalized);
                boolean matchesProvince = ward.getProvinceKey() != null && ward.getProvinceKey().contains(normalized);

                if (!matchesWard && !matchesDistrict && !matchesProvince) continue;

                // Build human-readable text and metadata. The metadata carries the legacy
                // numeric IDs the frontend needs to apply a real location filter (so a
                // LOCATION pick filters by province/district/ward instead of by free text).
                String displayText;
                Map<String, Object> meta = new HashMap<>();
                meta.put("provinceName", ward.getProvinceName());
                meta.put("districtName", ward.getDistrictName());
                meta.put("provinceCode", ward.getProvinceCode());
                meta.put("districtCode", ward.getDistrictCode());

                Integer legacyProvinceId = safeLegacyProvinceId(ward);
                Integer legacyDistrictId = safeLegacyDistrictId(ward);

                // The frontend uses these IDs to apply a real location filter.
                // Without them the only option is to fall back to a keyword
                // search, which is exactly the silent degradation we want to
                // prevent — skip the suggestion entirely instead.
                if (legacyProvinceId == null) {
                    log.warn("search-suggestions: dropping LOCATION suggestion without legacyProvinceId (ward={})", ward.getCode());
                    continue;
                }
                meta.put("legacyProvinceId", legacyProvinceId);

                String matchType;
                if (matchesWard) {
                    matchType = "WARD";
                    displayText = ward.getName() + ", " + ward.getDistrictName() + ", " + ward.getProvinceName();
                    meta.put("wardName", ward.getName());
                    meta.put("wardCode", ward.getCode());
                    meta.put("legacyWardId", ward.getId());
                    if (legacyDistrictId != null) meta.put("legacyDistrictId", legacyDistrictId);
                } else if (matchesDistrict) {
                    matchType = "DISTRICT";
                    displayText = ward.getDistrictName() + ", " + ward.getProvinceName();
                    if (legacyDistrictId != null) meta.put("legacyDistrictId", legacyDistrictId);
                } else {
                    matchType = "PROVINCE";
                    displayText = ward.getProvinceName();
                }
                meta.put("matchType", matchType);

                double score = WEIGHT_LOCATION * (1.0 - SCORE_DECAY_PER_RANK * items.size());

                items.add(SearchSuggestionItem.builder()
                        .type(SuggestionType.LOCATION)
                        .text(displayText)
                        .metadata(meta)
                        .score(score)
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("search-suggestions: location fetch failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Default LOCATION suggestions for empty / very short queries. Returns
     * the configured top cities (HCM, HN, ĐN, …) in their declared order so
     * the dropdown is useful as soon as the user focuses the search box.
     *
     * <p>Failures degrade silently to an empty list — this path runs on every
     * "focus before typing" event and must never break the UI.
     */
    private List<SearchSuggestionItem> fetchDefaultLocationSuggestions(int limit) {
        try {
            List<LegacyProvince> provinces = legacyProvinceRepository.findByCodeIn(DEFAULT_TOP_PROVINCE_CODES);
            if (provinces == null || provinces.isEmpty()) return Collections.emptyList();

            // Preserve the configured ordering (findByCodeIn returns DB order).
            Map<String, Integer> order = new HashMap<>();
            for (int i = 0; i < DEFAULT_TOP_PROVINCE_CODES.size(); i++) {
                order.put(DEFAULT_TOP_PROVINCE_CODES.get(i), i);
            }
            provinces.sort(Comparator.comparingInt(p -> order.getOrDefault(p.getCode(), Integer.MAX_VALUE)));

            int max = Math.min(limit, provinces.size());
            List<SearchSuggestionItem> items = new ArrayList<>(max);
            for (int i = 0; i < max; i++) {
                LegacyProvince p = provinces.get(i);

                // Same invariant as fetchLocationSuggestions: every LOCATION
                // suggestion must carry a legacyProvinceId. A null here would
                // mean a malformed seed in legacy_provinces — drop it rather
                // than emit a suggestion the frontend can't filter by.
                if (p.getId() == null) {
                    log.warn("search-suggestions: skipping default LOCATION without id (code={})", p.getCode());
                    continue;
                }

                Map<String, Object> meta = new HashMap<>();
                meta.put("matchType", "PROVINCE");
                meta.put("provinceName", p.getName());
                // districtName is part of the LocationSuggestionMetadata contract;
                // empty string keeps the FE renderer happy without inventing data.
                meta.put("districtName", "");
                if (p.getCode() != null) meta.put("provinceCode", p.getCode());
                meta.put("legacyProvinceId", p.getId());
                meta.put("isDefault", true);

                double score = WEIGHT_LOCATION * (1.0 - SCORE_DECAY_PER_RANK * items.size());

                items.add(SearchSuggestionItem.builder()
                        .type(SuggestionType.LOCATION)
                        .text(p.getName())
                        .metadata(meta)
                        .score(score)
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("search-suggestions: default top-city fetch failed (non-fatal): {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetches POPULAR_QUERY suggestions from the click telemetry table.
     * Only terms that contain the normalized query as a substring are returned,
     * so results remain contextually relevant to the current input.
     *
     * <p>On a cold-start deployment (empty telemetry table) this method silently
     * returns an empty list — no seeding required; TITLE and LOCATION suggestions
     * will fill the response.
     */
    private List<SearchSuggestionItem> fetchPopularQuerySuggestions(String normalized, int limit) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(POPULAR_QUERY_WINDOW_DAYS);
            List<Object[]> rows = clickRepository.findPopularQueryTexts(since, limit * 2); // fetch extra, filter below

            List<SearchSuggestionItem> items = new ArrayList<>();
            for (int i = 0; i < rows.size() && items.size() < limit; i++) {
                String text     = toString(rows.get(i)[0]);
                long   hitCount = toLongPrimitive(rows.get(i)[1]);

                if (text == null) continue;

                // Only include if the popular term is contextually relevant to the current query
                String termNorm = TextNormalizer.normalize(text);
                if (termNorm == null || !termNorm.contains(normalized)) continue;

                Map<String, Object> meta = new HashMap<>();
                meta.put("hitCount", hitCount);

                double score = WEIGHT_POPULAR_QUERY * (1.0 - SCORE_DECAY_PER_RANK * items.size());

                items.add(SearchSuggestionItem.builder()
                        .type(SuggestionType.POPULAR_QUERY)
                        .text(text)
                        .metadata(meta)
                        .score(score)
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("search-suggestions: popular-query fetch failed (non-fatal): {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — Merge & rank
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Merges three source lists, deduplicates by lowercased display text,
     * sorts by score descending, and trims to {@code limit}.
     */
    private List<SearchSuggestionItem> mergeAndRank(
            List<SearchSuggestionItem> titles,
            List<SearchSuggestionItem> locations,
            List<SearchSuggestionItem> popular,
            int limit) {

        // Use LinkedHashMap to preserve insertion order during dedup
        Map<String, SearchSuggestionItem> seen = new LinkedHashMap<>();

        for (SearchSuggestionItem item : titles)   dedup(seen, item);
        for (SearchSuggestionItem item : locations) dedup(seen, item);
        for (SearchSuggestionItem item : popular)   dedup(seen, item);

        List<SearchSuggestionItem> result = new ArrayList<>(seen.values());
        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // Return a concrete ArrayList — never the SubList view from List#subList.
        // The response is cached via GenericJackson2JsonRedisSerializer with
        // default typing enabled, so an ArrayList$SubList payload would write
        // "@class":"java.util.ArrayList$SubList" and fail to deserialize on
        // the next cache hit (no public no-arg constructor), leaking up as a
        // 9999 error and an empty dropdown on the client.
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    /** Inserts an item only if its dedup key (lowercased text) is not already present. */
    private void dedup(Map<String, SearchSuggestionItem> seen, SearchSuggestionItem item) {
        if (item.getText() == null) return;
        String key = item.getText().toLowerCase();
        seen.putIfAbsent(key, item);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — Telemetry writes (REQUIRES_NEW so they survive cache hits)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persists a {@link SearchQueryImpression} in a separate transaction.
     * Returns the generated ID (used by the controller to populate the response envelope).
     * Returns {@code 0} on failure so the API caller is never affected.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long persistImpression(
            String normalized, String provinceId, Long categoryId,
            int suggestionCount, String clientIp, String sessionId) {
        try {
            SearchQueryImpression impression = SearchQueryImpression.builder()
                    .queryNorm(normalized)
                    .provinceId(provinceId)
                    .categoryId(categoryId)
                    .suggestionCount(suggestionCount)
                    .clientIp(clientIp)
                    .sessionId(sessionId)
                    .build();
            return impressionRepository.save(impression).getId();
        } catch (Exception e) {
            log.warn("search-suggestions: impression persist failed (non-fatal): {}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * Persists a {@link SearchSuggestionClick} in a separate transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistClick(long impressionId, SuggestionType type, String text, Long listingId, int rank) {
        SearchSuggestionClick click = SearchSuggestionClick.builder()
                .impressionId(impressionId == 0 ? null : impressionId)
                .suggestionType(type.name())
                .suggestionText(text)
                .listingId(listingId)
                .rankPosition(rank)
                .build();
        clickRepository.save(click);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private — helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses the provinceId string as an Integer.
     * If the string is not numeric (e.g. new province code "01"), returns {@code null}
     * so the SQL query falls back to no province filter. The caller may handle new-code
     * filtering at the application layer in a future iteration.
     */
    private Integer parseProvinceId(String provinceId) {
        if (provinceId == null || provinceId.isBlank()) return null;
        try {
            return Integer.parseInt(provinceId.trim());
        } catch (NumberFormatException e) {
            log.debug("search-suggestions: provinceId '{}' is non-numeric, skipping legacy filter", provinceId);
            return null;
        }
    }

    /**
     * Converts a raw province ID string to a province code string for use with
     * {@link LegacyWardRepository#findByProvinceCode}.
     * Pads single-digit values with a leading zero to match the stored {@code province_code}.
     */
    private String toProvinceCode(String rawProvinceId) {
        if (rawProvinceId == null || rawProvinceId.isBlank()) return rawProvinceId;
        // If already looks like a code (e.g. "01", "79") return as-is
        if (rawProvinceId.length() == 2) return rawProvinceId;
        try {
            int val = Integer.parseInt(rawProvinceId.trim());
            return String.format("%02d", val);
        } catch (NumberFormatException e) {
            return rawProvinceId.trim();
        }
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l)   return l;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static long toLongPrimitive(Object o) {
        Long v = toLong(o);
        return v != null ? v : 0L;
    }

    private static String toString(Object o) {
        return o != null ? o.toString() : null;
    }

    /** Prefer the new-structure address when available. */
    private static String chooseAddress(String fullAddress, String fullNewAddress) {
        if (fullNewAddress != null && !fullNewAddress.isBlank()) return fullNewAddress;
        return fullAddress;
    }

    /**
     * Reads {@code ward.province.id} via the lazy relationship. Wrapped because we
     * never want telemetry-adjacent fetches to fail the suggestion response.
     */
    private static Integer safeLegacyProvinceId(LegacyWard ward) {
        try {
            return ward.getProvince() != null ? ward.getProvince().getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer safeLegacyDistrictId(LegacyWard ward) {
        try {
            return ward.getDistrict() != null ? ward.getDistrict().getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
