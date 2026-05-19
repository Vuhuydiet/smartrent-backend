package com.smartrent.service.discovery.impl;

import com.smartrent.dto.response.SearchSuggestionItem;
import com.smartrent.dto.response.SearchSuggestionsResponse;
import com.smartrent.enums.SuggestionType;
import com.smartrent.infra.repository.LegacyDistrictRepository;
import com.smartrent.infra.repository.LegacyProvinceRepository;
import com.smartrent.infra.repository.LegacyWardRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.service.discovery.SearchTelemetryWriter;
import com.smartrent.service.discovery.TelemetryExecutor;
import com.smartrent.infra.repository.SearchSuggestionClickRepository;
import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.LegacyProvince;
import com.smartrent.infra.repository.entity.LegacyWard;
import com.smartrent.infra.repository.entity.SearchQueryImpression;
import com.smartrent.infra.repository.entity.SearchSuggestionClick;
import com.smartrent.service.discovery.AmenityResolver;
import com.smartrent.service.discovery.LocationFuzzyIndex;
import com.smartrent.service.discovery.SearchSuggestionService;
import com.smartrent.util.SearchQueryParser;
import com.smartrent.util.SnowflakeId;
import com.smartrent.util.TextNormalizer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private static final double WEIGHT_TYPO          = 0.95;
    private static final double WEIGHT_PHONETIC      = 0.9;
    private static final double WEIGHT_POPULAR_QUERY = 0.8;
    private static final double SCORE_DECAY_PER_RANK = 0.05;

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

    /**
     * Curated "ready to apply" suggestions shown when the box is focused but
     * empty. Each carries pre-resolved structured filters (room + province
     * [+ district] + a 1–5 triệu price band) so the very first click runs a
     * useful filtered search instead of a bare city navigation. Order is the
     * display order; province-only rows first, then a few popular
     * district+city combos. {@code districtNameNorm} is
     * {@link TextNormalizer#normalize} output resolved to a legacy id via the
     * in-memory {@link LocationFuzzyIndex} (no DB round-trip).
     */
    private record DefaultIntent(String provinceCode, String districtNameNorm) {}

    private static final List<DefaultIntent> DEFAULT_INTENTS = List.of(
            new DefaultIntent("79", null),          // Hồ Chí Minh
            new DefaultIntent("01", null),          // Hà Nội
            new DefaultIntent("75", null),          // Đồng Nai
            new DefaultIntent("48", null),          // Đà Nẵng
            new DefaultIntent("79", "quan 7"),
            new DefaultIntent("79", "binh thanh"),
            new DefaultIntent("79", "tan binh"),
            new DefaultIntent("01", "cau giay")
    );

    private static final BigDecimal DEFAULT_MIN_PRICE = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal DEFAULT_MAX_PRICE = BigDecimal.valueOf(5_000_000);
    private static final String DEFAULT_PRODUCT_TYPE    = "ROOM";
    private static final String DEFAULT_PRODUCT_DISPLAY = "Nhà trọ";

    // ── Dependencies ─────────────────────────────────────────────────────────
    ListingRepository             listingRepository;
    LegacyWardRepository          legacyWardRepository;
    LegacyDistrictRepository      legacyDistrictRepository;
    LegacyProvinceRepository      legacyProvinceRepository;
    SearchSuggestionClickRepository clickRepository;
    TelemetryExecutor telemetryExecutor;
    SearchTelemetryWriter telemetryWriter;
    AmenityResolver amenityResolver;
    LocationFuzzyIndex locationFuzzyIndex;

    DoubleMetaphone metaphone = new DoubleMetaphone();
    LevenshteinDistance levenshtein = new LevenshteinDistance();

    private static final List<String> LOCAL_CANONICAL_SUGGESTIONS = List.of(
            "phòng trọ quận 1 dưới 5 triệu",
            "căn hộ quận 1 giá dưới 5 triệu",
            "căn hộ gần đại học quốc gia",
            "căn hộ full nội thất gần đại học quốc gia",
            "phòng trọ có máy lạnh",
            "phòng trọ full nội thất có máy lạnh",
            "phòng full nội thất bình thạnh",
            "nhà trọ giá rẻ quận 7",
            "phòng trọ quận 7 giá rẻ",
            "căn hộ bình thạnh full nội thất",
            "studio gần trung tâm",
            "phòng trọ thủ đức gần đại học quốc gia"
    );

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

        String rawNormalized = TextNormalizer.normalize(query);
        String normalized = normalizeIntent(rawNormalized);
        if (normalized == null || normalized.length() < 2) {
            // Empty / very short query → return curated top-city LOCATION
            // suggestions so the dropdown is useful on focus before typing.
            log.debug("search-suggestions: query too short, returning curated default suggestions");
            List<SearchSuggestionItem> defaults = buildDefaultContextualSuggestions(safeLimit);
            if (defaults.isEmpty()) {
                // Degrade to plain top-city LOCATION rows so the dropdown is
                // never empty (e.g. legacy_provinces unseeded in a fresh env).
                defaults = fetchDefaultLocationSuggestions(safeLimit);
            }
            long impressionId = dispatchImpression(
                    Objects.toString(normalized, ""), provinceId, categoryId,
                    defaults.size(), clientIp, sessionId);
            return SearchSuggestionsResponse.builder()
                    .suggestions(defaults)
                    .queryNorm(Objects.toString(normalized, ""))
                    .impressionId(impressionId)
                    .build();
        }

        Integer provinceIdInt = parseProvinceId(provinceId);

        // Parse the free-text query into structured filters locally (no AI).
        // The isolated location token drives LOCATION matching, so a multi-word
        // query like "tro tan binh duoi 5tr" still resolves the "Tân Bình"
        // district instead of LIKE-matching the whole sentence against a name.
        SearchQueryParser.ParsedQuery parsed = SearchQueryParser.parse(query);
        String locationQuery = (parsed.locationText() != null && !parsed.locationText().isBlank())
                ? parsed.locationText()
                : normalized;

        // ── Fetch candidates from all sources ────────────────────────────────
        // No synchronous AI round-trip here: the local parser + DB location
        // resolution + live amenities table already produce the structured
        // appliedFilters the frontend needs, so the AI call was pure latency
        // (a blocking Feign hop with a 30s read timeout) for no functional
        // gain. The dropdown is built entirely from in-process + indexed-DB
        // sources now.
        List<SearchSuggestionItem> titleItems    = fetchTitleSuggestions(normalized, provinceIdInt, categoryId, safeLimit);
        List<SearchSuggestionItem> locationItems = fetchLocationSuggestions(locationQuery, provinceId);
        List<SearchSuggestionItem> popularItems  = fetchPopularQuerySuggestions(normalized, safeLimit);
        List<SearchSuggestionItem> typoItems     = fetchTypoSuggestions(normalized, safeLimit);
        // The phonetic DB lookup is a `LIKE '%...%'` scan on phonetic_title
        // (non-sargable, full scan). It is only a fuzzy fallback for
        // misspellings the FULLTEXT title source could not catch, so skip the
        // scan whenever we already have title hits — that keeps it off the
        // hot path for the overwhelming majority of queries.
        List<SearchSuggestionItem> phoneticItems = fetchPhoneticSuggestions(
                normalized, provinceIdInt, categoryId, safeLimit, titleItems.isEmpty());

        // ── Merge, deduplicate, rank, and trim ───────────────────────────────
        List<SearchSuggestionItem> merged = mergeAndRank(
                titleItems, locationItems, popularItems, typoItems, phoneticItems, safeLimit);

        // ── Synthesize a "ready to apply" suggestion from parsed filters ─────
        // Guarantees the dropdown is never empty for NL queries and gives the
        // frontend a structured payload to auto-apply on submit.
        Map<String, Object> appliedFilters = parsed.toAppliedFilters();
        SearchSuggestionItem synthesized =
                synthesizeParsedSuggestion(parsed, merged, locationItems, appliedFilters);
        // Authoritative amenity-id resolution against the LIVE amenities table.
        // Source phrases = the locally-parsed amenity display names.
        // AmenityResolver maps them to whatever ids the backend actually
        // seeded, so "có điều hoà" filters by the real "Điều hòa" id.
        resolveAmenityIdsFromLiveTable(appliedFilters, parsed);
        // Drop the parsed free-text keys: `keyword` (residual) and
        // `locationText` (unresolved place). Both would make the frontend run
        // a title FULLTEXT search off an error-prone parse — exactly the
        // behaviour this feature replaces. appliedFilters stays structured-only.
        appliedFilters.remove("keyword");
        appliedFilters.remove("locationText");
        // Only surface the "ready to apply" suggestion + the appliedFilters
        // payload when at least one STRUCTURED filter resolved — an applied
        // filter that carries nothing actionable is meaningless to the user.
        boolean hasStructuredFilters = hasStructuredAppliedFilters(appliedFilters);
        SearchSuggestionItem contextual = hasStructuredFilters ? synthesized : null;
        List<SearchSuggestionItem> finalList = withSynthesizedFirst(contextual, merged, safeLimit);

        // ── Dispatch impression telemetry off the request thread ────────────
        long impressionId = dispatchImpression(normalized, provinceId, categoryId, finalList.size(), clientIp, sessionId);

        log.debug("search-suggestions: q='{}' → {} suggestions (impression {})", normalized, finalList.size(), impressionId);

        return SearchSuggestionsResponse.builder()
                .suggestions(finalList)
                .queryNorm(normalized)
                .impressionId(impressionId)
                .appliedFilters(hasStructuredFilters ? appliedFilters : null)
                .build();
    }

    /**
     * Keys that represent a real, actionable filter the frontend can apply to
     * {@code POST /v1/listings/search}. Deliberately excludes {@code keyword},
     * {@code locationText} and the {@code amenities} display list.
     */
    private static final java.util.Set<String> STRUCTURED_FILTER_KEYS = java.util.Set.of(
            "productType", "productTypes", "listingType",
            "minPrice", "maxPrice", "minArea", "maxArea", "bedrooms",
            "legacyProvinceId", "legacyDistrictId", "legacyWardId",
            "provinceCode", "districtCode", "wardCode",
            "amenityIds");

    private boolean hasStructuredAppliedFilters(Map<String, Object> appliedFilters) {
        if (appliedFilters == null || appliedFilters.isEmpty()) return false;
        for (String key : STRUCTURED_FILTER_KEYS) {
            Object v = appliedFilters.get(key);
            if (v == null) continue;
            if (v instanceof java.util.Collection<?> c && c.isEmpty()) continue;
            if (v instanceof String s && s.isBlank()) continue;
            return true;
        }
        return false;
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
     * Delegates to {@link com.smartrent.util.SearchTextCanonicalizer#applyIntent}
     * — the single source of truth shared with the suggestion cache key, so the
     * key and the computation can never drift apart.
     */
    private String normalizeIntent(String normalized) {
        return com.smartrent.util.SearchTextCanonicalizer.applyIntent(normalized);
    }

    private List<SearchSuggestionItem> fetchTypoSuggestions(String normalized, int limit) {
        List<SearchSuggestionItem> items = new ArrayList<>();
        for (String candidate : LOCAL_CANONICAL_SUGGESTIONS) {
            String candidateNorm = TextNormalizer.normalize(candidate);
            if (candidateNorm == null) continue;

            int distance = levenshtein.apply(compactKey(normalized), compactKey(candidateNorm));
            boolean contains = candidateNorm.contains(normalized) || normalized.contains(candidateNorm);
            boolean close = distance > 0 && distance <= Math.max(2, Math.min(5, normalized.length() / 4));
            if (!contains && !close) continue;

            double bonus = contains ? 0.15 : 0.0;
            double score = WEIGHT_TYPO + bonus - (0.03 * items.size());
            items.add(buildTextSuggestion(SuggestionType.TYPO_CORRECTION, candidate, score, "LOCAL_DICTIONARY"));
            if (items.size() >= limit) break;
        }
        return items;
    }

    private List<SearchSuggestionItem> fetchPhoneticSuggestions(
            String normalized, Integer provinceIdInt, Long categoryId, int limit, boolean allowDbScan) {
        String phoneticQuery = toPhoneticQuery(normalized);
        if (phoneticQuery == null || phoneticQuery.length() < 2) {
            return Collections.emptyList();
        }

        List<SearchSuggestionItem> items = new ArrayList<>();
        for (String candidate : LOCAL_CANONICAL_SUGGESTIONS) {
            String candidatePhonetic = toPhoneticQuery(TextNormalizer.normalize(candidate));
            if (candidatePhonetic != null && candidatePhonetic.contains(phoneticQuery)) {
                items.add(buildTextSuggestion(
                        SuggestionType.PHONETIC, candidate,
                        WEIGHT_PHONETIC - (0.03 * items.size()), "LOCAL_PHONETIC"));
            }
            if (items.size() >= limit) return items;
        }

        // Skip the non-sargable phonetic_title scan entirely when the caller
        // already has FULLTEXT title hits — phonetic is only the misspelling
        // fallback, and the scan is the most expensive query in this path.
        if (!allowDbScan) {
            return items;
        }

        try {
            List<Object[]> rows = listingRepository.findPhoneticTitleSuggestions(
                    phoneticQuery, provinceIdInt, categoryId, limit);
            for (Object[] row : rows) {
                Map<String, Object> meta = new HashMap<>();
                String addr = chooseAddress(toString(row[2]), toString(row[3]));
                if (addr != null) meta.put("address", addr);
                meta.put("matchType", "DB_PHONETIC");

                items.add(SearchSuggestionItem.builder()
                        .type(SuggestionType.PHONETIC)
                        .text(toString(row[1]))
                        .listingId(toLong(row[0]))
                        .metadata(meta)
                        .score(WEIGHT_PHONETIC - (0.03 * items.size()))
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("search-suggestions: phonetic fetch failed (non-fatal): {}", e.getMessage(), e);
            return items;
        }
    }

    /**
     * Resolve amenity ids from the live {@code amenities} table — the single
     * source of truth. Phrases = locally-parsed amenities ∪ the AI's amenity
     * display names (e.g. "Điều hòa"). Sets {@code amenityIds} +
     * {@code amenityMatchMode} when anything resolves, and clears both
     * otherwise so a stale / placeholder id can never reach the frontend.
     */
    private void resolveAmenityIdsFromLiveTable(
            Map<String, Object> appliedFilters, SearchQueryParser.ParsedQuery parsed) {
        java.util.Set<String> phrases = new java.util.LinkedHashSet<>();
        if (parsed != null && parsed.amenities() != null) {
            phrases.addAll(parsed.amenities());
        }
        Object aiAmenities = appliedFilters.get("amenities");
        if (aiAmenities instanceof java.util.Collection<?> c) {
            for (Object o : c) {
                if (o != null && !o.toString().isBlank()) phrases.add(o.toString());
            }
        }

        if (!phrases.isEmpty()) {
            AmenityResolver.Resolved resolved = amenityResolver.resolve(phrases);
            if (!resolved.amenityIds().isEmpty()) {
                appliedFilters.put("amenityIds", new ArrayList<>(resolved.amenityIds()));
                appliedFilters.put("amenityMatchMode", "ALL");
                return;
            }
        }
        appliedFilters.remove("amenityIds");
        appliedFilters.remove("amenityMatchMode");
    }

    private SearchSuggestionItem buildTextSuggestion(SuggestionType type, String text, double score, String matchType) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("matchType", matchType);
        return SearchSuggestionItem.builder()
                .type(type)
                .text(text)
                .metadata(meta)
                .score(score)
                .build();
    }

    private String toPhoneticQuery(String normalized) {
        if (normalized == null || normalized.isBlank()) return null;
        StringBuilder result = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            if (word.length() < 2) continue;
            String code = metaphone.doubleMetaphone(word);
            if (code == null || code.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(code);
        }
        return result.isEmpty() ? null : result.toString();
    }

    /**
     * Fetches TITLE suggestions from the listings table via a lightweight native projection.
     * Fetches up to {@code limit} rows; the final merged list is trimmed later.
     */
    private List<SearchSuggestionItem> fetchTitleSuggestions(
            String normalized, Integer provinceIdInt, Long categoryId, int limit) {
        try {
            String fulltextQuery = toContentFulltextQuery(normalized);
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
     * Words that describe price / intent rather than title content. Forcing
     * these into the AND full-text query is what made multi-word NL queries
     * (e.g. "... duoi 5 trieu") match zero titles, since no listing title
     * contains "duoi" or "trieu". They are dropped here so the title source
     * matches on the meaningful tokens ("phong tro tan binh") instead.
     */
    private static final java.util.Set<String> FT_STOPWORDS = java.util.Set.of(
            "duoi", "tren", "tu", "den", "khoang", "tam", "gia", "re",
            "trieu", "tr", "ty", "nghin", "ngan", "cu", "gan", "khong",
            "qua", "toi", "da", "it", "nhat", "co", "va", "can", "tim"
    );

    /**
     * Builds the title full-text query from content words only. Falls back to
     * {@link #toBooleanFulltextQuery} when every token was a stopword (e.g. the
     * user typed only a price), so early single-word keystrokes still work.
     */
    private String toContentFulltextQuery(String normalized) {
        if (normalized == null || normalized.isBlank()) return null;

        StringBuilder query = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            if (word.length() < 2) continue;
            if (FT_STOPWORDS.contains(word)) continue;
            if (word.matches("\\d+")) continue;
            if (!query.isEmpty()) query.append(' ');
            query.append('+').append(word).append('*');
        }

        return query.isEmpty() ? toBooleanFulltextQuery(normalized) : query.toString();
    }

    /**
     * Turns the locally-parsed filters into a single top-ranked suggestion the
     * user can click to run the search with filters already applied. When a
     * LOCATION candidate was found it reuses that accented name (and its legacy
     * ids, copied into {@code appliedFilters}) so the displayed text is clean;
     * otherwise it falls back to the parser's reconstructed phrase. Returns
     * {@code null} when the query carried no recognisable filter intent.
     */
    private SearchSuggestionItem synthesizeParsedSuggestion(
            SearchQueryParser.ParsedQuery parsed,
            List<SearchSuggestionItem> merged,
            List<SearchSuggestionItem> locationItems,
            Map<String, Object> appliedFilters) {

        if (parsed == null || !parsed.hasStructuredFilter()) return null;

        // Resolve the parsed location into legacy ids from the FULL untrimmed
        // location candidate list — not the display-trimmed `merged` list.
        // Otherwise a query like "phòng trọ tân bình dưới 5 triệu" whose
        // "Tân Bình" district candidate gets pushed out of the top-N by
        // higher-scored TITLE hits would leave `appliedFilters.locationText`
        // unresolved, degrading the location into a fuzzy keyword instead of
        // a real province/district filter. Prefer the location the user
        // actually sees (first LOCATION in `merged`) for the display label,
        // else fall back to the best untrimmed candidate so the ids still
        // resolve even when no LOCATION row is shown.
        SearchSuggestionItem bestLocation = firstLocation(merged);
        if (bestLocation == null) {
            bestLocation = firstLocation(locationItems);
        }

        String productPart = vnProductTypeDisplay(parsed.productType());
        String locationPart;

        if (bestLocation != null) {
            locationPart = bestLocation.getText();
            copyLocationIds(bestLocation.getMetadata(), appliedFilters);
        } else {
            locationPart = parsed.locationText();
        }

        StringBuilder text = new StringBuilder();
        if (productPart != null) text.append(productPart);
        if (locationPart != null && !locationPart.isBlank()) {
            if (!text.isEmpty()) text.append(' ');
            text.append(locationPart);
        }
        String priceClause = priceClause(parsed.minPrice(), parsed.maxPrice());
        if (priceClause != null) {
            if (!text.isEmpty()) text.append(' ');
            text.append(priceClause);
        }
        for (String a : parsed.amenities()) {
            if (!text.isEmpty()) text.append(' ');
            text.append(a);
        }

        String display = text.length() == 0 ? parsed.fallbackSuggestion() : text.toString();
        if (display == null || display.isBlank()) return null;

        Map<String, Object> meta = new HashMap<>();
        meta.put("matchType", "PARSED_QUERY");
        meta.put("appliedFilters", appliedFilters);

        return SearchSuggestionItem.builder()
                .type(SuggestionType.AI_INTENT)
                .text(display)
                .metadata(meta)
                .score(WEIGHT_TITLE + 1.0) // always rank first
                .build();
    }

    /** First LOCATION item in a candidate list, or {@code null} if none. */
    private SearchSuggestionItem firstLocation(List<SearchSuggestionItem> items) {
        if (items == null) return null;
        return items.stream()
                .filter(i -> i.getType() == SuggestionType.LOCATION)
                .findFirst()
                .orElse(null);
    }

    private void copyLocationIds(Map<String, Object> locationMeta, Map<String, Object> appliedFilters) {
        if (locationMeta == null) return;
        for (String key : new String[]{
                "legacyProvinceId", "legacyDistrictId", "legacyWardId",
                "provinceCode", "districtCode", "wardCode"}) {
            Object v = locationMeta.get(key);
            if (v != null) appliedFilters.put(key, v);
        }
    }

    private List<SearchSuggestionItem> withSynthesizedFirst(
            SearchSuggestionItem synthesized, List<SearchSuggestionItem> merged, int limit) {

        if (synthesized == null) return merged;

        List<SearchSuggestionItem> result = new ArrayList<>(merged.size() + 1);
        result.add(synthesized);
        String synthKey = synthesized.getText().toLowerCase();
        for (SearchSuggestionItem item : merged) {
            if (item.getText() != null && item.getText().toLowerCase().equals(synthKey)) continue;
            result.add(item);
        }
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    private static String vnProductTypeDisplay(String productType) {
        if (productType == null) return null;
        return switch (productType) {
            case "ROOM"      -> "phòng trọ";
            case "APARTMENT" -> "căn hộ";
            case "HOUSE"     -> "nhà";
            case "STUDIO"    -> "studio";
            case "OFFICE"    -> "văn phòng";
            default          -> null;
        };
    }

    private static String priceClause(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) return "từ " + humanPrice(min) + " đến " + humanPrice(max);
        if (max != null) return "dưới " + humanPrice(max);
        if (min != null) return "trên " + humanPrice(min);
        return null;
    }

    private static String humanPrice(BigDecimal v) {
        long n = v.longValue();
        if (n >= 1_000_000_000L) return strip(n / 1_000_000_000d) + " tỷ";
        if (n >= 1_000_000L)     return strip(n / 1_000_000d) + " triệu";
        if (n >= 1_000L)         return strip(n / 1_000d) + " nghìn";
        return String.valueOf(n);
    }

    private static String strip(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    /**
     * Per-tier candidate caps. Province lives at the top because it dedupes the
     * fastest (one row per matching province); district and ward sit below to
     * give finer-grained picks when the user drills down.
     */
    private static final int MAX_PROVINCE_CANDIDATES = 4;
    private static final int MAX_DISTRICT_CANDIDATES = 4;
    private static final int MAX_WARD_CANDIDATES     = 4;

    /**
     * Similarity floor for the typo fallback. 0.86 Jaro-Winkler keeps
     * "tn binh"→"tan binh" / "ha noi"→"ha noi" while rejecting unrelated
     * names; tuned to favour precision (a wrong location is worse than none).
     */
    private static final double FUZZY_LOCATION_MIN_SCORE =
            LocationFuzzyIndex.DEFAULT_MIN_SCORE;

    /**
     * Multi-tier LOCATION suggestion fetch. Queries the province, district and
     * ward tables independently — each tier only filters on its own columns,
     * so a province-level match no longer drags every district/ward inside it
     * into the result set.
     *
     * <p>Why two key forms (see {@code compactKey}): the seed migrations left
     * {@code *_key} columns in a space-stripped, partially-stripped form
     * (e.g. {@code "thanhphohochiminh"}, {@code "quan1"}, {@code "phng_phc_x"}),
     * which {@code TextNormalizer.normalize}'s "ho chi minh" / "quan 1" output
     * cannot LIKE against. Each repository query ORs the {@code name} branch
     * (with spaces, leans on the DB's accent-insensitive collation) against a
     * {@code key} branch (no spaces) so either layout matches without needing
     * a data backfill.
     */
    private List<SearchSuggestionItem> fetchLocationSuggestions(String normalized, String rawProvinceId) {
        // Structured fast path: a multi-segment "<district> <province>" phrase
        // (e.g. "quan 1 ho chi minh") resolves precisely off the in-memory
        // index with NO DB round-trip. A confident DISTRICT hit IS the answer
        // for "search theo quận" — return only it and skip the three
        // unindexed LIKE '%...%' tier scans + the fuzzy scan. This both fixes
        // the "ra kết quả lạ" bug (the LIKE path matched "Quảng Ninh / Nam /
        // Ngãi" for "quận …") and removes 4 scans per debounced keystroke.
        LocationFuzzyIndex.Match seg =
                locationFuzzyIndex.resolveLocationPhrase(normalized).orElse(null);
        if (seg != null && seg.kind() == LocationFuzzyIndex.Kind.DISTRICT) {
            return new ArrayList<>(List.of(toLocationItem(seg, 0, false)));
        }

        String compact = compactKey(normalized);
        List<SearchSuggestionItem> items = new ArrayList<>();
        // A province-only structured hit is still authoritative — keep it
        // first, then let the DB tiers add finer-grained alternatives.
        if (seg != null) {
            items.add(toLocationItem(seg, 0, false));
        }
        items.addAll(fetchProvinceSuggestionsTier(normalized, compact));
        items.addAll(fetchDistrictSuggestionsTier(normalized, compact, rawProvinceId));
        items.addAll(fetchWardSuggestionsTier(normalized, compact, rawProvinceId));
        // Typo fallback: only when the exact tiers found nothing — keeps the
        // fuzzy scorer off the hot path for well-formed queries. Handles
        // "tn binh" → Tân Bình (+ its parent province), so a misspelt place
        // still resolves to province + district instead of degrading to a
        // keyword search.
        if (items.isEmpty()) {
            items.addAll(fetchFuzzyLocationSuggestions(normalized));
        }
        return items;
    }

    /**
     * Maps {@link LocationFuzzyIndex} hits into LOCATION suggestion items,
     * mirroring the exact-tier metadata contract so the synthesized
     * "ready to apply" row and the frontend resolve province + district ids
     * exactly as they would for an exact match.
     */
    private List<SearchSuggestionItem> fetchFuzzyLocationSuggestions(String normalized) {
        List<LocationFuzzyIndex.Match> matches = locationFuzzyIndex.search(
                normalized, FUZZY_LOCATION_MIN_SCORE, MAX_DISTRICT_CANDIDATES);
        if (matches.isEmpty()) return Collections.emptyList();

        List<SearchSuggestionItem> items = new ArrayList<>(matches.size());
        for (LocationFuzzyIndex.Match m : matches) {
            items.add(toLocationItem(m, items.size(), true));
        }
        return items;
    }

    /**
     * Maps a {@link LocationFuzzyIndex.Match} to a LOCATION suggestion item.
     * Shared by the structured fast path (exact, {@code fuzzy=false}) and the
     * typo fallback ({@code fuzzy=true}) so both emit the identical metadata
     * contract the synthesized "ready to apply" row and the frontend rely on
     * ({@code legacyProvinceId}/{@code legacyDistrictId} + province/district
     * codes — these feed {@code resolveAddressMappings}, which expands the
     * legacy district id to the new ward codes so listings under BOTH the old
     * and new address structures are matched).
     */
    private SearchSuggestionItem toLocationItem(
            LocationFuzzyIndex.Match m, int rank, boolean fuzzy) {
        boolean isDistrict = m.kind() == LocationFuzzyIndex.Kind.DISTRICT;

        Map<String, Object> meta = new HashMap<>();
        meta.put("matchType", isDistrict ? "DISTRICT" : "PROVINCE");
        meta.put("provinceName", m.provinceName());
        meta.put("districtName", isDistrict ? m.districtName() : "");
        if (m.provinceCode() != null) meta.put("provinceCode", m.provinceCode());
        if (m.legacyProvinceId() != null) meta.put("legacyProvinceId", m.legacyProvinceId());
        if (isDistrict) {
            if (m.districtCode() != null) meta.put("districtCode", m.districtCode());
            if (m.legacyDistrictId() != null) meta.put("legacyDistrictId", m.legacyDistrictId());
        }
        if (fuzzy) meta.put("fuzzy", true);

        String text = isDistrict
                ? m.districtName() + ", " + m.provinceName()
                : m.provinceName();
        double score = WEIGHT_LOCATION * (1.0 - SCORE_DECAY_PER_RANK * rank);
        return SearchSuggestionItem.builder()
                .type(SuggestionType.LOCATION)
                .text(text)
                .metadata(meta)
                .score(score)
                .build();
    }

    /**
     * Province-level matches. Cheapest tier (~63 rows, indexed on key/name) and
     * the one that fixes the most common reported miss ("Hồ Chí Minh" → empty).
     */
    private List<SearchSuggestionItem> fetchProvinceSuggestionsTier(String normalized, String compact) {
        try {
            List<LegacyProvince> provinces = legacyProvinceRepository.findSuggestionMatches(
                    normalized, compact, PageRequest.of(0, MAX_PROVINCE_CANDIDATES));

            List<SearchSuggestionItem> items = new ArrayList<>(provinces.size());
            for (LegacyProvince p : provinces) {
                if (p.getId() == null) {
                    log.warn("search-suggestions: dropping PROVINCE suggestion without id (code={})", p.getCode());
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
            log.warn("search-suggestions: province tier fetch failed (non-fatal): {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * District-level matches. Falls back to a province-scoped query when the
     * caller already locked the user into a province (so "Quận" doesn't return
     * districts from every other province).
     */
    private List<SearchSuggestionItem> fetchDistrictSuggestionsTier(String normalized, String compact, String rawProvinceId) {
        try {
            PageRequest page = PageRequest.of(0, MAX_DISTRICT_CANDIDATES);
            List<District> districts = (rawProvinceId != null && !rawProvinceId.isBlank())
                    ? legacyDistrictRepository.findSuggestionMatchesByProvince(
                            toProvinceCode(rawProvinceId), normalized, compact, page)
                    : legacyDistrictRepository.findSuggestionMatches(normalized, compact, page);

            List<SearchSuggestionItem> items = new ArrayList<>(districts.size());
            for (District d : districts) {
                Integer legacyProvinceId = safeLegacyProvinceIdOf(d);
                if (legacyProvinceId == null) {
                    log.warn("search-suggestions: dropping DISTRICT suggestion without legacyProvinceId (district={})", d.getCode());
                    continue;
                }

                Map<String, Object> meta = new HashMap<>();
                meta.put("matchType", "DISTRICT");
                meta.put("provinceName", d.getProvinceName());
                meta.put("districtName", d.getName());
                if (d.getProvince() != null && d.getProvince().getCode() != null) {
                    meta.put("provinceCode", d.getProvince().getCode());
                }
                if (d.getCode() != null) meta.put("districtCode", d.getCode());
                meta.put("legacyProvinceId", legacyProvinceId);
                if (d.getId() != null) meta.put("legacyDistrictId", d.getId());

                String displayText = d.getName() + ", " + d.getProvinceName();
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
            log.warn("search-suggestions: district tier fetch failed (non-fatal): {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Ward-level matches. Uses the v2 ward query (ward-only columns) so a
     * city-name search no longer pulls 8 arbitrary wards inside the city as
     * "WARD" suggestions — those came from the old ward-only fetch matching
     * on the denormalized province/district key columns.
     */
    private List<SearchSuggestionItem> fetchWardSuggestionsTier(String normalized, String compact, String rawProvinceId) {
        try {
            PageRequest page = PageRequest.of(0, MAX_WARD_CANDIDATES);
            List<LegacyWard> wards = (rawProvinceId != null && !rawProvinceId.isBlank())
                    ? legacyWardRepository.findWardSuggestionMatchesByProvince(
                            toProvinceCode(rawProvinceId), normalized, compact, page)
                    : legacyWardRepository.findWardSuggestionMatches(normalized, compact, page);

            List<SearchSuggestionItem> items = new ArrayList<>(wards.size());
            for (LegacyWard ward : wards) {
                Integer legacyProvinceId = safeLegacyProvinceId(ward);
                Integer legacyDistrictId = safeLegacyDistrictId(ward);
                if (legacyProvinceId == null) {
                    log.warn("search-suggestions: dropping WARD suggestion without legacyProvinceId (ward={})", ward.getCode());
                    continue;
                }

                Map<String, Object> meta = new HashMap<>();
                meta.put("matchType", "WARD");
                meta.put("provinceName", ward.getProvinceName());
                meta.put("districtName", ward.getDistrictName());
                meta.put("wardName", ward.getName());
                if (ward.getProvinceCode() != null) meta.put("provinceCode", ward.getProvinceCode());
                if (ward.getDistrictCode() != null) meta.put("districtCode", ward.getDistrictCode());
                if (ward.getCode() != null) meta.put("wardCode", ward.getCode());
                meta.put("legacyProvinceId", legacyProvinceId);
                if (legacyDistrictId != null) meta.put("legacyDistrictId", legacyDistrictId);
                if (ward.getId() != null) meta.put("legacyWardId", ward.getId());

                String displayText = ward.getName() + ", " + ward.getDistrictName() + ", " + ward.getProvinceName();
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
            log.warn("search-suggestions: ward tier fetch failed (non-fatal): {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Strip whitespace from a normalized query so it can LIKE against the
     * legacy {@code *_key} columns, whose seed values dropped spaces
     * (e.g. {@code "thanhphohochiminh"}, {@code "quan1"}). Returns "" rather
     * than null so the LIKE still produces a deterministic miss instead of
     * an NPE if the caller forgot to pre-validate.
     */
    private static String compactKey(String normalized) {
        return normalized == null ? "" : normalized.replace(" ", "");
    }

    /**
     * Builds the curated "ready to apply" default suggestions
     * ({@link #DEFAULT_INTENTS}). Each row is an AI_INTENT item carrying its
     * own {@code appliedFilters} (room + province [+ district] + a 1–5 triệu
     * band) in metadata, exactly like the synthesized contextual row, so the
     * frontend applies structured filters on click instead of doing a bare
     * city navigation. District ids are resolved from the in-memory
     * {@link LocationFuzzyIndex} (no DB hit); a row whose province (or named
     * district) cannot be resolved is skipped rather than emitting a
     * suggestion the frontend cannot filter by.
     *
     * <p>Never throws — degrades to an empty list so the caller can fall back.
     */
    private List<SearchSuggestionItem> buildDefaultContextualSuggestions(int limit) {
        try {
            Map<String, LegacyProvince> byCode = new HashMap<>();
            for (LegacyProvince p : legacyProvinceRepository.findByCodeIn(DEFAULT_TOP_PROVINCE_CODES)) {
                if (p.getCode() != null) byCode.put(p.getCode(), p);
            }
            if (byCode.isEmpty()) return Collections.emptyList();

            String priceClause = priceClause(DEFAULT_MIN_PRICE, DEFAULT_MAX_PRICE);
            List<SearchSuggestionItem> items = new ArrayList<>();
            for (DefaultIntent intent : DEFAULT_INTENTS) {
                if (items.size() >= limit) break;
                LegacyProvince province = byCode.get(intent.provinceCode());
                if (province == null || province.getId() == null) continue;

                Map<String, Object> applied = new LinkedHashMap<>();
                applied.put("productType", DEFAULT_PRODUCT_TYPE);
                applied.put("minPrice", DEFAULT_MIN_PRICE);
                applied.put("maxPrice", DEFAULT_MAX_PRICE);
                applied.put("legacyProvinceId", province.getId());
                if (province.getCode() != null) applied.put("provinceCode", province.getCode());

                String locationLabel = province.getShortName();
                if (intent.districtNameNorm() != null) {
                    LocationFuzzyIndex.Match d = locationFuzzyIndex
                            .resolveDistrict(intent.provinceCode(), intent.districtNameNorm())
                            .orElse(null);
                    if (d == null || d.legacyDistrictId() == null) continue; // can't filter → skip
                    applied.put("legacyDistrictId", d.legacyDistrictId());
                    if (d.districtCode() != null) applied.put("districtCode", d.districtCode());
                    locationLabel = d.districtName() + ", " + province.getShortName();
                }

                StringBuilder text = new StringBuilder(DEFAULT_PRODUCT_DISPLAY)
                        .append(' ').append(locationLabel);
                if (priceClause != null) text.append(' ').append(priceClause);

                Map<String, Object> meta = new HashMap<>();
                meta.put("matchType", "PARSED_QUERY");
                meta.put("appliedFilters", applied);
                meta.put("isDefault", true);

                double score = (WEIGHT_TITLE + 1.0) - (SCORE_DECAY_PER_RANK * items.size());
                items.add(SearchSuggestionItem.builder()
                        .type(SuggestionType.AI_INTENT)
                        .text(text.toString())
                        .metadata(meta)
                        .score(score)
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("search-suggestions: default contextual build failed (non-fatal): {}", e.getMessage(), e);
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
            List<SearchSuggestionItem> typo,
            List<SearchSuggestionItem> phonetic,
            int limit) {

        // Use LinkedHashMap to preserve insertion order during dedup
        Map<String, SearchSuggestionItem> seen = new LinkedHashMap<>();

        for (SearchSuggestionItem item : titles)   dedup(seen, item);
        for (SearchSuggestionItem item : locations) dedup(seen, item);
        for (SearchSuggestionItem item : typo)      dedup(seen, item);
        for (SearchSuggestionItem item : phonetic)  dedup(seen, item);
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
     * Generates the impression id up front (so the synchronous response can
     * carry it for click-tracking) and dispatches the actual INSERT to
     * {@link TelemetryExecutor} — the request thread never waits on the
     * telemetry write. Never throws; never returns 0 (the id always exists,
     * independent of whether the async write later succeeds — the telemetry
     * tables have no FK, so a late/failed impression row is harmless).
     */
    private long dispatchImpression(
            String normalized, String provinceId, Long categoryId,
            int suggestionCount, String clientIp, String sessionId) {
        long id = SnowflakeId.next();
        SearchQueryImpression impression = SearchQueryImpression.builder()
                .id(id)
                .queryNorm(normalized)
                .provinceId(provinceId)
                .categoryId(categoryId)
                .suggestionCount(suggestionCount)
                .clientIp(clientIp)
                .sessionId(sessionId)
                .build();
        telemetryExecutor.execute(() -> telemetryWriter.saveImpression(impression));
        return id;
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

    private static Integer safeLegacyProvinceIdOf(District district) {
        try {
            return district.getProvince() != null ? district.getProvince().getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
