package com.smartrent.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the location-parsing behaviour that the "quận tân bình" suggestion
 * bug fix depends on: a district marker must be stripped from a NAMED
 * district so it resolves against the legacy short name/key, while a
 * NUMBERED district must keep its marker so it still matches the
 * {@code quanN} key.
 */
class SearchQueryParserTest {

    @Test
    @DisplayName("'quận tân bình' drops the district marker → location 'tan binh'")
    void namedDistrictMarkerStripped() {
        SearchQueryParser.ParsedQuery parsed = SearchQueryParser.parse("quận tân bình");
        assertEquals("tan binh", parsed.locationText());
        assertTrue(parsed.hasStructuredFilter());
    }

    @Test
    @DisplayName("'huyện bình chánh' drops the marker → location 'binh chanh'")
    void namedHuyenMarkerStripped() {
        assertEquals("binh chanh",
                SearchQueryParser.parse("huyện bình chánh").locationText());
    }

    @Test
    @DisplayName("'thị xã dĩ an' drops the marker → location 'di an'")
    void thiXaMarkerStripped() {
        assertEquals("di an",
                SearchQueryParser.parse("thị xã dĩ an").locationText());
    }

    @Test
    @DisplayName("bare 'tân bình' is unchanged → location 'tan binh'")
    void bareNamedDistrictUnchanged() {
        assertEquals("tan binh",
                SearchQueryParser.parse("tân bình").locationText());
    }

    @Test
    @DisplayName("numbered district 'quận 1' keeps its marker (no over-strip to '1')")
    void numberedDistrictKeepsMarker() {
        // The digit is dropped by stopword removal (pre-existing behaviour);
        // the marker MUST survive so the location is still "quan" and not
        // an empty / digit-only token.
        assertEquals("quan", SearchQueryParser.parse("quận 1").locationText());
    }

    @Test
    @DisplayName("full NL query: type + price + marker-stripped district all resolve")
    void fullNaturalLanguageQuery() {
        SearchQueryParser.ParsedQuery parsed =
                SearchQueryParser.parse("phòng trọ quận tân bình dưới 5 triệu");
        assertEquals("ROOM", parsed.productType());
        assertEquals(0, new BigDecimal("5000000").compareTo(parsed.maxPrice()));
        assertEquals("tan binh", parsed.locationText());
    }

    @Test
    @DisplayName("range with destroyed dash: 'từ 10-20tr' → 10tr..20tr")
    void priceRangeDashStripped() {
        SearchQueryParser.ParsedQuery parsed =
                SearchQueryParser.parse("phòng trọ từ 10-20tr");
        assertEquals("ROOM", parsed.productType());
        assertEquals(0, new BigDecimal("10000000").compareTo(parsed.minPrice()));
        assertEquals(0, new BigDecimal("20000000").compareTo(parsed.maxPrice()));
    }

    @Test
    @DisplayName("range spelled out: 'từ 10 đến 20 tr' → 10tr..20tr")
    void priceRangeSpelledOut() {
        SearchQueryParser.ParsedQuery parsed =
                SearchQueryParser.parse("từ 10 đến 20 tr");
        assertEquals(0, new BigDecimal("10000000").compareTo(parsed.minPrice()));
        assertEquals(0, new BigDecimal("20000000").compareTo(parsed.maxPrice()));
    }

    @Test
    @DisplayName("dual-unit range with no cue: '10tr-20tr' → 10tr..20tr")
    void priceRangeDualUnitNoCue() {
        SearchQueryParser.ParsedQuery parsed = SearchQueryParser.parse("10tr-20tr");
        assertEquals(0, new BigDecimal("10000000").compareTo(parsed.minPrice()));
        assertEquals(0, new BigDecimal("20000000").compareTo(parsed.maxPrice()));
    }

    @Test
    @DisplayName("empty query parses to an all-null result")
    void emptyQuery() {
        SearchQueryParser.ParsedQuery parsed = SearchQueryParser.parse("   ");
        assertNull(parsed.locationText());
        assertNull(parsed.productType());
        assertTrue(parsed.amenities().isEmpty());
    }
}
