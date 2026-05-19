package com.smartrent.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single source of truth for the two query canonicalizations the
 * search-suggestion path depends on, and the {@link #cacheCanonical} form
 * derived from them.
 *
 * <h3>Why this exists</h3>
 * A suggestion response is a deterministic function of exactly two derived
 * strings:
 * <ul>
 *   <li>{@link #applyIntent}{@code (}{@link TextNormalizer#normalize}{@code )} —
 *       drives the TITLE / POPULAR_QUERY / TYPO / PHONETIC sources and the
 *       {@code normalized} value used throughout
 *       {@code SearchSuggestionServiceImpl};</li>
 *   <li>{@link SearchQueryParser#expandAbbreviations}{@code (}{@link
 *       TextNormalizer#normalize}{@code )} — drives
 *       {@link SearchQueryParser#parse} (location + appliedFilters).</li>
 * </ul>
 *
 * <p>The old suggestion cache key folded only case/accents
 * ({@link TextNormalizer#normalize}), so abbreviation variants of the SAME
 * intent — {@code "q1"} vs {@code "quan 1"}, {@code "5tr"} vs
 * {@code "5 trieu"}, {@code "phongtro"} vs {@code "phong tro"} — produced
 * distinct Redis keys and each one re-ran the full DB resolution (title
 * FULLTEXT + popular-query scan + parse). Keying off {@link #cacheCanonical}
 * collapses inputs that provably yield an identical response into one entry,
 * while never merging non-equivalent inputs (the two parts together uniquely
 * determine the result), so it raises the hit rate with zero change to what
 * the API returns.
 *
 * <p>The intent transform lived as a private method in
 * {@code SearchSuggestionServiceImpl}; it is centralised here so the cache key
 * and the computation can never drift apart (a drift would let the cache serve
 * the wrong list for a key).
 */
public final class SearchTextCanonicalizer {

    private SearchTextCanonicalizer() {}

    /**
     * Separator between the two canonical parts. {@code U+0001} is a control
     * char that {@link TextNormalizer#normalize} / {@link SearchQueryParser}
     * output can never contain, so the two parts are unambiguously delimited.
     */
    private static final char PART_SEP = '\u0001';

    private static final Pattern PRICE_TRIEU_PATTERN =
            Pattern.compile("\\b(\\d{1,3})\\s*(tr|trieu)\\b");

    /**
     * Intent normalization. Input MUST already be {@link TextNormalizer#normalize}
     * output (accent-stripped, lowercase). Expands the colloquial spellings the
     * suggestion service canonicalizes before matching titles / popular terms.
     *
     * <p>This is the verbatim algorithm previously inlined in
     * {@code SearchSuggestionServiceImpl.normalizeIntent} — behaviour is
     * unchanged; only its home moved.
     */
    public static String applyIntent(String normalized) {
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

    /**
     * Canonical cache discriminator for a RAW suggestion query. Two raw queries
     * with the same value here are guaranteed to produce the same suggestion
     * response, so they can safely share a cached entry.
     */
    public static String cacheCanonical(String rawQuery) {
        String normalized = TextNormalizer.normalize(rawQuery);
        String intent = applyIntent(normalized);
        String expanded = SearchQueryParser.expandAbbreviations(normalized);
        return safe(intent) + PART_SEP + safe(expanded);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
