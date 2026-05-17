package com.smartrent.service.discovery.impl;

import com.smartrent.dto.request.AiSuggestionRequest;
import com.smartrent.dto.response.AiSuggestionResponse;
import com.smartrent.dto.response.SearchSuggestionItem;
import com.smartrent.dto.response.SearchSuggestionsResponse;
import com.smartrent.enums.SuggestionType;
import com.smartrent.infra.client.AiServerClient;
import com.smartrent.infra.repository.LegacyDistrictRepository;
import com.smartrent.infra.repository.LegacyProvinceRepository;
import com.smartrent.infra.repository.LegacyWardRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.SearchQueryImpressionRepository;
import com.smartrent.infra.repository.SearchSuggestionClickRepository;
import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.LegacyProvince;
import com.smartrent.infra.repository.entity.LegacyWard;
import com.smartrent.infra.repository.entity.SearchQueryImpression;
import com.smartrent.infra.repository.entity.SearchSuggestionClick;
import com.smartrent.service.discovery.AmenityResolver;
import com.smartrent.service.discovery.SearchSuggestionService;
import com.smartrent.util.SearchQueryParser;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final double WEIGHT_AI_INTENT     = 1.15;
    private static final double WEIGHT_LOCATION      = 1.0;
    private static final double WEIGHT_TYPO          = 0.95;
    private static final double WEIGHT_PHONETIC      = 0.9;
    private static final double WEIGHT_POPULAR_QUERY = 0.8;
    private static final double SCORE_DECAY_PER_RANK = 0.05;

    /** Rolling window for popular-query aggregation (7 days). */
    private static final long POPULAR_QUERY_WINDOW_DAYS = 7;
    private static final int MIN_AI_SUGGESTION_LENGTH = 5;
    private static final Pattern PRICE_TRIEU_PATTERN = Pattern.compile("\\b(\\d{1,3})\\s*(tr|trieu)\\b");

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
    LegacyDistrictRepository      legacyDistrictRepository;
    LegacyProvinceRepository      legacyProvinceRepository;
    SearchQueryImpressionRepository impressionRepository;
    SearchSuggestionClickRepository clickRepository;
    AiServerClient aiServerClient;
    AmenityResolver amenityResolver;

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

        // Parse the free-text query into structured filters locally (no AI).
        // The isolated location token drives LOCATION matching, so a multi-word
        // query like "tro tan binh duoi 5tr" still resolves the "Tân Bình"
        // district instead of LIKE-matching the whole sentence against a name.
        SearchQueryParser.ParsedQuery parsed = SearchQueryParser.parse(query);
        String locationQuery = (parsed.locationText() != null && !parsed.locationText().isBlank())
                ? parsed.locationText()
                : normalized;

        // ── Fetch candidates from all sources ────────────────────────────────
        List<SearchSuggestionItem> titleItems    = fetchTitleSuggestions(normalized, provinceIdInt, categoryId, safeLimit);
        List<SearchSuggestionItem> locationItems = fetchLocationSuggestions(locationQuery, provinceId);
        List<SearchSuggestionItem> popularItems  = fetchPopularQuerySuggestions(normalized, safeLimit);
        List<SearchSuggestionItem> typoItems     = fetchTypoSuggestions(normalized, safeLimit);
        List<SearchSuggestionItem> phoneticItems = fetchPhoneticSuggestions(normalized, provinceIdInt, categoryId, safeLimit);

        // ── Merge, deduplicate, rank, and trim ───────────────────────────────
        List<SearchSuggestionItem> merged = mergeAndRank(
                titleItems, locationItems, popularItems, typoItems, phoneticItems, Collections.emptyList(), safeLimit);
        AiSuggestionResponse aiResp = null;
        if (shouldCallAiForSuggestions(normalized, merged, safeLimit)) {
            aiResp = fetchAiSuggestionResponse(query, safeLimit);
            List<SearchSuggestionItem> aiItems = toAiIntentItems(aiResp, normalized, safeLimit);
            merged = mergeAndRank(titleItems, locationItems, popularItems, typoItems, phoneticItems, aiItems, safeLimit);
        }

        // ── Synthesize a "ready to apply" suggestion from parsed filters ─────
        // Guarantees the dropdown is never empty for NL queries and gives the
        // frontend a structured payload to auto-apply on submit.
        Map<String, Object> appliedFilters = parsed.toAppliedFilters();
        // Enrich with canonical amenity ids so the frontend can auto-apply the
        // amenity filter chips (the real /v1/listings/search filter takes
        // amenityIds + amenityMatchMode, not the display text). The displayed
        // suggestion text and the `amenities` display list are left unchanged,
        // so the response structure is preserved — only enriched.
        if (parsed.amenities() != null && !parsed.amenities().isEmpty()) {
            AmenityResolver.Resolved resolvedAmenities = amenityResolver.resolve(parsed.amenities());
            if (!resolvedAmenities.amenityIds().isEmpty()) {
                appliedFilters.put("amenityIds", new ArrayList<>(resolvedAmenities.amenityIds()));
                appliedFilters.put("amenityMatchMode", "ALL");
            }
        }
        SearchSuggestionItem synthesized =
                synthesizeParsedSuggestion(parsed, merged, locationItems, appliedFilters);
        // The AI server resolved the query against its RAG knowledge base
        // (same context the chatbox uses): location → legacy ids, amenity →
        // ids, type → enum. Overlay those AFTER the local/DB resolution so the
        // RAG-resolved ids win. `synthesized` stored this same map reference
        // in its metadata, so both the dropdown's apply-row and the top-level
        // response reflect the overlay. Without this the frontend kept
        // FULLTEXT-searching the raw query (the reported bug).
        overlayAiAppliedFilters(appliedFilters, aiResp);
        List<SearchSuggestionItem> finalList = withSynthesizedFirst(synthesized, merged, safeLimit);

        // ── Persist impression (separate TX — does not affect cache result) ──
        long impressionId = persistImpression(normalized, provinceId, categoryId, finalList.size(), clientIp, sessionId);

        log.debug("search-suggestions: q='{}' → {} suggestions (impression {})", normalized, finalList.size(), impressionId);

        return SearchSuggestionsResponse.builder()
                .suggestions(finalList)
                .queryNorm(normalized)
                .impressionId(impressionId)
                .appliedFilters(appliedFilters.isEmpty() ? null : appliedFilters)
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

    private String normalizeIntent(String normalized) {
        if (normalized == null || normalized.isBlank()) return null;

        String result = " " + normalized + " ";
        result = result.replace(" canho ", " can ho ");
        result = result.replace(" phongtro ", " phong tro ");
        result = result.replace(" nha tro ", " phong tro ");
        result = result.replace(" tro ", " phong tro ");
        result = result.replace(" dhqg ", " dai hoc quoc gia ");
        result = result.replace(" may lan ", " may lanh ");
        result = result.replace(" maylanh ", " may lanh ");
        result = result.replace(" full nt ", " full noi that ");
        result = result.replace(" noi that day du ", " full noi that ");

        for (int i = 1; i <= 12; i++) {
            result = result.replace(" q" + i + " ", " quan " + i + " ");
        }

        Matcher matcher = PRICE_TRIEU_PATTERN.matcher(result);
        result = matcher.replaceAll("$1 trieu");

        return result.replaceAll("\\s+", " ").trim();
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
            String normalized, Integer provinceIdInt, Long categoryId, int limit) {
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

    private boolean shouldCallAiForSuggestions(String normalized, List<SearchSuggestionItem> localItems, int limit) {
        if (normalized == null || normalized.length() < MIN_AI_SUGGESTION_LENGTH) return false;
        if (localItems.size() < Math.min(3, limit)) return true;
        return normalized.contains(" gan ") || normalized.contains(" duoi ") || normalized.contains(" tren ");
    }

    /**
     * Single AI server round-trip. Returns the raw response (text suggestions
     * + RAG-resolved {@code appliedFilters}) or {@code null} on any failure —
     * the AI is strictly an enrichment, never required for a usable response.
     */
    private AiSuggestionResponse fetchAiSuggestionResponse(String rawQuery, int limit) {
        try {
            return aiServerClient.suggestSearchQueries(AiSuggestionRequest.builder()
                    .query(rawQuery)
                    .limit(Math.min(limit, 5))
                    .build());
        } catch (Exception e) {
            log.debug("search-suggestions: AI suggestion call skipped: {}", e.getMessage());
            return null;
        }
    }

    /** Maps the AI server's text suggestions into AI_INTENT dropdown rows. */
    private List<SearchSuggestionItem> toAiIntentItems(
            AiSuggestionResponse response, String normalized, int limit) {
        if (response == null || response.getSuggestions() == null) {
            return Collections.emptyList();
        }

        List<SearchSuggestionItem> items = new ArrayList<>();
        for (String text : response.getSuggestions()) {
            if (text == null || text.isBlank()) continue;
            String itemNorm = TextNormalizer.normalize(text);
            if (itemNorm == null) continue;

            boolean related = itemNorm.contains(normalized)
                    || normalized.split("\\s+").length >= 2
                    || levenshtein.apply(compactKey(normalized), compactKey(itemNorm)) <= 5;
            if (!related) continue;

            items.add(buildTextSuggestion(
                    SuggestionType.AI_INTENT, text,
                    WEIGHT_AI_INTENT - (0.04 * items.size()), "AI_INTENT"));
            if (items.size() >= limit) break;
        }
        return items;
    }

    /**
     * Overlay the AI server's RAG-resolved filters onto the locally-parsed
     * {@code appliedFilters}, letting the AI win for any key it resolved.
     * Null / blank / empty AI values are skipped so a sparse AI payload never
     * clobbers a good local value.
     */
    private void overlayAiAppliedFilters(Map<String, Object> target, AiSuggestionResponse aiResp) {
        if (aiResp == null || aiResp.getAppliedFilters() == null) return;
        for (Map.Entry<String, Object> e : aiResp.getAppliedFilters().entrySet()) {
            Object v = e.getValue();
            if (v == null) continue;
            if (v instanceof java.util.Collection<?> c && c.isEmpty()) continue;
            if (v instanceof String s && s.isBlank()) continue;
            target.put(e.getKey(), v);
        }
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
        String compact = compactKey(normalized);
        List<SearchSuggestionItem> items = new ArrayList<>();
        items.addAll(fetchProvinceSuggestionsTier(normalized, compact));
        items.addAll(fetchDistrictSuggestionsTier(normalized, compact, rawProvinceId));
        items.addAll(fetchWardSuggestionsTier(normalized, compact, rawProvinceId));
        return items;
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
            List<SearchSuggestionItem> aiIntent,
            int limit) {

        // Use LinkedHashMap to preserve insertion order during dedup
        Map<String, SearchSuggestionItem> seen = new LinkedHashMap<>();

        for (SearchSuggestionItem item : titles)   dedup(seen, item);
        for (SearchSuggestionItem item : locations) dedup(seen, item);
        for (SearchSuggestionItem item : aiIntent)  dedup(seen, item);
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

    private static Integer safeLegacyProvinceIdOf(District district) {
        try {
            return district.getProvince() != null ? district.getProvince().getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
