package com.smartrent.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, AI-free parser that turns a Vietnamese free-text rental query
 * into structured filters. It runs entirely in-process (regex + small
 * dictionaries) so it is cheap enough to call on every suggestion request and
 * works even when the AI server is unreachable.
 *
 * <p>It is intentionally <em>not</em> a general NLP pipeline. It recognises the
 * handful of patterns that actually matter for this platform:
 * <ul>
 *   <li>property type — "phòng trọ" → ROOM, "căn hộ" → APARTMENT, …</li>
 *   <li>listing type — "thuê" → RENT (default), "bán" → SALE, "ở ghép" → SHARE</li>
 *   <li>price — "dưới 5tr", "trên 3 triệu", "từ 2 đến 4 triệu", "5 triệu", "1 tỷ"</li>
 *   <li>amenities — "máy lạnh", "full nội thất", …</li>
 *   <li>location — whatever meaningful tokens remain after the above are removed</li>
 * </ul>
 *
 * <p>All matching is done on the accent-stripped output of
 * {@link TextNormalizer#normalize(String)} plus abbreviation expansion
 * (q1 → quan 1, dhqg → dai hoc quoc gia, 5tr → 5 trieu), so it is robust to the
 * way Vietnamese users actually type into a search box.
 */
public final class SearchQueryParser {

    private SearchQueryParser() {}

    /** Parsed result. {@code null} fields mean "not specified in the query". */
    public record ParsedQuery(
            String productType,
            String listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String locationText,
            List<String> amenities,
            String residualKeyword,
            String fallbackSuggestion
    ) {
        public boolean hasStructuredFilter() {
            return productType != null || listingType != null
                    || minPrice != null || maxPrice != null
                    || (locationText != null && !locationText.isBlank())
                    || !amenities.isEmpty();
        }

        public Map<String, Object> toAppliedFilters() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (productType != null)   m.put("productType", productType);
            if (listingType != null)   m.put("listingType", listingType);
            if (minPrice != null)      m.put("minPrice", minPrice);
            if (maxPrice != null)      m.put("maxPrice", maxPrice);
            if (locationText != null && !locationText.isBlank()) m.put("locationText", locationText);
            if (!amenities.isEmpty())  m.put("amenities", amenities);
            if (residualKeyword != null && !residualKeyword.isBlank()) m.put("keyword", residualKeyword);
            return m;
        }
    }

    // ── Property type dictionary (phrase → [enum, vietnamese display]) ───────
    // Order matters: longer / more specific phrases first so "nha tro" is read
    // as ROOM before the bare "nha" → HOUSE rule can fire.
    private static final Map<String, String[]> PRODUCT_TYPES = new LinkedHashMap<>();
    static {
        PRODUCT_TYPES.put("phong tro",       new String[]{"ROOM",      "phòng trọ"});
        PRODUCT_TYPES.put("nha tro",         new String[]{"ROOM",      "phòng trọ"});
        PRODUCT_TYPES.put("can ho",          new String[]{"APARTMENT", "căn hộ"});
        PRODUCT_TYPES.put("chung cu",        new String[]{"APARTMENT", "căn hộ"});
        PRODUCT_TYPES.put("nha nguyen can",  new String[]{"HOUSE",     "nhà nguyên căn"});
        PRODUCT_TYPES.put("van phong",       new String[]{"OFFICE",    "văn phòng"});
        PRODUCT_TYPES.put("mat bang",        new String[]{"OFFICE",    "mặt bằng"});
        PRODUCT_TYPES.put("studio",          new String[]{"STUDIO",    "studio"});
        PRODUCT_TYPES.put("tro",             new String[]{"ROOM",      "phòng trọ"});
        PRODUCT_TYPES.put("nha",             new String[]{"HOUSE",     "nhà"});
    }

    private static final Map<String, String> LISTING_TYPES = new LinkedHashMap<>();
    static {
        LISTING_TYPES.put("o ghep",    "SHARE");
        LISTING_TYPES.put("can ban",   "SALE");
        LISTING_TYPES.put("ban",       "SALE");
        LISTING_TYPES.put("can thue",  "RENT");
        LISTING_TYPES.put("cho thue",  "RENT");
        LISTING_TYPES.put("thue",      "RENT");
    }

    // amenity normalized-phrase → vietnamese display
    private static final Map<String, String> AMENITIES = new LinkedHashMap<>();
    static {
        AMENITIES.put("full noi that",  "full nội thất");
        AMENITIES.put("day du noi that","full nội thất");
        AMENITIES.put("noi that",       "nội thất");
        AMENITIES.put("may lanh",       "máy lạnh");
        AMENITIES.put("dieu hoa",       "máy lạnh");
        AMENITIES.put("may giat",       "máy giặt");
        AMENITIES.put("tu lanh",        "tủ lạnh");
        AMENITIES.put("thang may",      "thang máy");
        AMENITIES.put("ban cong",       "ban công");
        AMENITIES.put("gac",            "gác");
        AMENITIES.put("wifi",           "wifi");
        AMENITIES.put("cho de xe",      "chỗ để xe");
        AMENITIES.put("bao ve",         "bảo vệ");
        AMENITIES.put("nha bep",        "nhà bếp");
        AMENITIES.put("ve sinh rieng",  "vệ sinh riêng");
        AMENITIES.put("gio giac tu do", "giờ giấc tự do");
    }

    /** Tokens that are never part of a location and carry no filter on their own. */
    private static final Set<String> STOPWORDS = Set.of(
            "gan", "o", "tai", "khu", "vuc", "vung", "quanh", "khu vuc",
            "gia", "re", "gia re", "dep", "moi", "rong", "rai", "co", "va",
            "can", "tim", "thue", "muon", "mua", "phong", "nha", "cho",
            "duoi", "tren", "tu", "den", "khoang", "tam", "gia tu",
            "khong", "qua", "toi", "da", "it", "nhat", "duoi muc"
    );

    private static final Pattern RANGE_PATTERN =
            Pattern.compile("(?:tu\\s+)?(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)?\\s*(?:-|den|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)");
    private static final Pattern MAX_PATTERN =
            Pattern.compile("(?:duoi|khong qua|toi da|<=|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)?");
    private static final Pattern MIN_PATTERN =
            Pattern.compile("(?:tren|tu|it nhat|>=|>)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)?");
    private static final Pattern LONE_PRICE_PATTERN =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)");

    public static ParsedQuery parse(String rawQuery) {
        String norm = expandAbbreviations(TextNormalizer.normalize(rawQuery));
        if (norm == null || norm.isBlank()) {
            return new ParsedQuery(null, null, null, null, null, List.of(), null, null);
        }

        String working = " " + norm + " ";

        // 1. Price first — it owns the most ambiguous tokens (numbers, "tr").
        BigDecimal[] price = new BigDecimal[2]; // [min, max]
        working = extractPrice(working, price);

        // 2. Property type.
        String productType = null;
        String productDisplay = null;
        for (Map.Entry<String, String[]> e : PRODUCT_TYPES.entrySet()) {
            String phrase = " " + e.getKey() + " ";
            if (working.contains(phrase)) {
                productType = e.getValue()[0];
                productDisplay = e.getValue()[1];
                working = working.replace(phrase, " ");
                break;
            }
        }

        // 3. Listing type (default RENT, applied by the caller, not forced here).
        String listingType = null;
        for (Map.Entry<String, String> e : LISTING_TYPES.entrySet()) {
            String phrase = " " + e.getKey() + " ";
            if (working.contains(phrase)) {
                listingType = e.getValue();
                working = working.replace(phrase, " ");
                break;
            }
        }

        // 4. Amenities (multiple allowed).
        List<String> amenities = new ArrayList<>();
        for (Map.Entry<String, String> e : AMENITIES.entrySet()) {
            String phrase = " " + e.getKey() + " ";
            if (working.contains(phrase)) {
                amenities.add(e.getValue());
                working = working.replace(phrase, " ");
            }
        }

        // 5. Whatever is left, minus stopwords, is the location guess. The
        // leftover is treated as a location, not a free keyword, so we do not
        // also emit it as residualKeyword (that would double-filter).
        String locationText = stripStopwords(working);
        String fallback = buildFallbackSuggestion(productDisplay, locationText, price[0], price[1], amenities);

        return new ParsedQuery(
                productType, listingType, price[0], price[1],
                locationText.isBlank() ? null : locationText,
                amenities, null, fallback);
    }

    /**
     * Expands the abbreviations Vietnamese users type in a search box so the
     * rest of the parser only deals with one canonical spelling.
     */
    public static String expandAbbreviations(String normalized) {
        if (normalized == null || normalized.isBlank()) return normalized;
        String r = " " + normalized + " ";
        r = r.replace(" canho ", " can ho ");
        r = r.replace(" chungcu ", " chung cu ");
        r = r.replace(" phongtro ", " phong tro ");
        r = r.replace(" nhatro ", " nha tro ");
        r = r.replace(" dhqg ", " dai hoc quoc gia ");
        r = r.replace(" dh ", " dai hoc ");
        r = r.replace(" kdc ", " khu dan cu ");
        r = r.replace(" tt ", " trung tam ");
        r = r.replace(" maylanh ", " may lanh ");
        r = r.replace(" may lan ", " may lanh ");
        r = r.replace(" dieuhoa ", " dieu hoa ");
        r = r.replace(" full nt ", " full noi that ");
        r = r.replace(" fullnt ", " full noi that ");
        r = r.replace(" nt ", " noi that ");
        for (int i = 1; i <= 12; i++) {
            r = r.replace(" q" + i + " ", " quan " + i + " ");
        }
        r = r.replace(" tphcm ", " ho chi minh ");
        r = r.replace(" hcm ", " ho chi minh ");
        r = r.replace(" sg ", " ho chi minh ");
        r = r.replace(" hn ", " ha noi ");
        // "5tr" / "5 tr" → "5 trieu" (keep the unit so price parsing can read it)
        r = r.replaceAll("(\\d+)\\s*tr\\b", "$1 trieu");
        r = r.replaceAll("(\\d+)\\s*ty\\b", "$1 ty");
        r = r.replaceAll("(\\d+)\\s*k\\b", "$1 nghin");
        return r.replaceAll("\\s+", " ").trim();
    }

    private static String extractPrice(String working, BigDecimal[] out) {
        Matcher range = RANGE_PATTERN.matcher(working);
        if (range.find()) {
            String unit = range.group(4) != null ? range.group(4) : range.group(2);
            out[0] = money(range.group(1), unit);
            out[1] = money(range.group(3), unit);
            return working.replace(range.group(), " ");
        }

        Matcher max = MAX_PATTERN.matcher(working);
        if (max.find()) {
            out[1] = money(max.group(1), max.group(2));
            working = working.replace(max.group(), " ");
        }

        Matcher min = MIN_PATTERN.matcher(working);
        if (min.find()) {
            out[0] = money(min.group(1), min.group(2));
            working = working.replace(min.group(), " ");
        }

        if (out[0] == null && out[1] == null) {
            Matcher lone = LONE_PRICE_PATTERN.matcher(working);
            if (lone.find()) {
                // A bare "5 triệu" with no comparator → treat as an upper bound,
                // which matches how users mean "around / up to" in practice.
                out[1] = money(lone.group(1), lone.group(2));
                working = working.replace(lone.group(), " ");
            }
        }
        return working;
    }

    private static BigDecimal money(String number, String unit) {
        if (number == null) return null;
        BigDecimal value = new BigDecimal(number.replace(",", "."));
        long multiplier = switch (unit == null ? "" : unit) {
            case "ty"                       -> 1_000_000_000L;
            case "trieu", "tr", "cu"        -> 1_000_000L;
            case "nghin", "ngan", "k"       -> 1_000L;
            default                          -> 1_000_000L; // bare number in a rental context ≈ triệu
        };
        return value.multiply(BigDecimal.valueOf(multiplier));
    }

    private static String stripStopwords(String working) {
        StringBuilder sb = new StringBuilder();
        for (String token : working.trim().split("\\s+")) {
            if (token.isBlank()) continue;
            if (STOPWORDS.contains(token)) continue;
            if (token.matches("\\d+")) continue;
            if (token.length() < 2) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(token);
        }
        return sb.toString().trim();
    }

    /**
     * Human-readable suggestion used when no DB location matches, so the
     * dropdown still shows the user's intent back to them.
     */
    private static String buildFallbackSuggestion(
            String productDisplay, String locationText,
            BigDecimal minPrice, BigDecimal maxPrice, List<String> amenities) {

        Set<String> parts = new LinkedHashSet<>();
        if (productDisplay != null) parts.add(productDisplay);
        if (locationText != null && !locationText.isBlank()) parts.add(locationText);
        if (maxPrice != null && minPrice != null) {
            parts.add("từ " + humanPrice(minPrice) + " đến " + humanPrice(maxPrice));
        } else if (maxPrice != null) {
            parts.add("dưới " + humanPrice(maxPrice));
        } else if (minPrice != null) {
            parts.add("trên " + humanPrice(minPrice));
        }
        parts.addAll(amenities);
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private static String humanPrice(BigDecimal v) {
        long n = v.longValue();
        if (n >= 1_000_000_000L) return trim(n / 1_000_000_000d) + " tỷ";
        if (n >= 1_000_000L)     return trim(n / 1_000_000d) + " triệu";
        if (n >= 1_000L)         return trim(n / 1_000d) + " nghìn";
        return String.valueOf(n);
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}