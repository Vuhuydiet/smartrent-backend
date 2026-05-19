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
    @DisplayName("numbered district 'quận 1' keeps BOTH marker and number")
    void numberedDistrictKeepsMarkerAndNumber() {
        // Regression: the digit used to be dropped by stopword removal,
        // collapsing "quan 1" → "quan", which then LIKE-matched
        // "Quảng Ninh / Quảng Nam / Quảng Ngãi" (the reported
        // "search theo quận ra kết quả lạ" bug). The number MUST survive
        // so the district resolves to "Quận 1" (key "quan1").
        assertEquals("quan 1", SearchQueryParser.parse("quận 1").locationText());
    }

    @Test
    @DisplayName("multi-digit district 'quận 12' keeps its number")
    void twoDigitDistrictKept() {
        assertEquals("quan 12", SearchQueryParser.parse("quận 12").locationText());
    }

    @Test
    @DisplayName("full NL query with NUMBERED district + city")
    void fullNaturalLanguageNumberedDistrict() {
        SearchQueryParser.ParsedQuery parsed =
                SearchQueryParser.parse("nhà trọ quận 1 hồ chí minh dưới 5tr");
        assertEquals("ROOM", parsed.productType());
        assertEquals(0, new BigDecimal("5000000").compareTo(parsed.maxPrice()));
        // Number preserved next to the city so the segment resolver can
        // split it into Quận 1 + Hồ Chí Minh.
        assertEquals("quan 1 ho chi minh", parsed.locationText());
    }

    @Test
    @DisplayName("a long bare number (year) is still dropped from the location")
    void longNumberStillDropped() {
        // Only 1–2 digit district numbers survive; a 4-digit number is noise.
        assertEquals("tan binh",
                SearchQueryParser.parse("phòng trọ tân bình 2024").locationText());
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
