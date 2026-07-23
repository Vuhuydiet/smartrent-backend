package com.smartrent.service.news.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * News slugs used to be generated with {@code replaceAll("[^a-z0-9\\s-]", "")},
 * which stripped every Vietnamese diacritic character (and {@code đ}) outright,
 * so "Bất động sản..." became the unreadable "bt-ng-sn...". Locks in that the
 * generator transliterates Vietnamese to ASCII instead of deleting it.
 */
class NewsServiceImplSlugTest {

    private final NewsServiceImpl service = new NewsServiceImpl(null, null, null);

    @Test
    void transliteratesVietnameseDiacriticsToAsciiInsteadOfStrippingThem() {
        String title = "Bất động sản năm 2026: Đầu tư bằng kiến thức, không phải cảm xúc";

        assertEquals(
                "bat-dong-san-nam-2026-dau-tu-bang-kien-thuc-khong-phai-cam-xuc",
                service.generateSlug(title));
    }

    @Test
    void mapsDStrokeAndCollapsesPunctuationAndEdgeSeparators() {
        // "đ"/"Đ" have no NFD decomposition; punctuation becomes stripped and
        // surrounding whitespace must collapse to a single "-" with no leading
        // or trailing separator.
        assertEquals(
                "dong-nai-quy-i-2026",
                service.generateSlug("  Đồng Nai:  Quý I / 2026!  "));
    }
}
