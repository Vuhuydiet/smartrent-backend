package com.smartrent.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The suggestion cache key contract: abbreviation variants of the SAME intent
 * MUST canonicalize to the same string (so they share one Redis entry instead
 * of each re-running the DB resolution), while genuinely different intents MUST
 * stay distinct (so the cache never serves the wrong list for a key).
 */
class SearchTextCanonicalizerTest {

    @Test
    @DisplayName("accent / case / abbreviation variants collapse to one cache key")
    void equivalentVariantsCollapse() {
        String canonical = SearchTextCanonicalizer.cacheCanonical("quan 1 ho chi minh duoi 5 trieu");

        // accents + casing
        assertEquals(canonical,
                SearchTextCanonicalizer.cacheCanonical("Quận 1 Hồ Chí Minh dưới 5 triệu"));
        // "q1" abbreviation + "5tr" price shorthand
        assertEquals(canonical,
                SearchTextCanonicalizer.cacheCanonical("q1 hồ chí minh dưới 5tr"));
        // extra whitespace
        assertEquals(canonical,
                SearchTextCanonicalizer.cacheCanonical("  quan   1  ho chi minh   duoi 5tr "));
    }

    @Test
    @DisplayName("'phongtro' and 'phong tro' share a cache key")
    void productTypeAbbreviationCollapses() {
        assertEquals(
                SearchTextCanonicalizer.cacheCanonical("phongtro quan 7"),
                SearchTextCanonicalizer.cacheCanonical("phòng trọ quận 7"));
    }

    @Test
    @DisplayName("different districts / provinces / types do NOT collapse")
    void distinctIntentsStayDistinct() {
        assertNotEquals(
                SearchTextCanonicalizer.cacheCanonical("quan 1 ho chi minh"),
                SearchTextCanonicalizer.cacheCanonical("quan 2 ho chi minh"));
        assertNotEquals(
                SearchTextCanonicalizer.cacheCanonical("ha noi"),
                SearchTextCanonicalizer.cacheCanonical("ho chi minh"));
        assertNotEquals(
                SearchTextCanonicalizer.cacheCanonical("phong tro quan 1"),
                SearchTextCanonicalizer.cacheCanonical("can ho quan 1"));
        assertNotEquals(
                SearchTextCanonicalizer.cacheCanonical("quan 1 duoi 5 trieu"),
                SearchTextCanonicalizer.cacheCanonical("quan 1 duoi 7 trieu"));
    }

    @Test
    @DisplayName("null / blank queries canonicalize without throwing")
    void nullSafe() {
        assertEquals(
                SearchTextCanonicalizer.cacheCanonical(null),
                SearchTextCanonicalizer.cacheCanonical("   "));
    }

    @Test
    @DisplayName("applyIntent stays the verbatim transform (q1 → quan 1, 5tr → 5 trieu)")
    void applyIntentBehaviourPreserved() {
        assertEquals("quan 1 ho chi minh duoi 5 trieu",
                SearchTextCanonicalizer.applyIntent("q1 ho chi minh duoi 5tr"));
    }
}
