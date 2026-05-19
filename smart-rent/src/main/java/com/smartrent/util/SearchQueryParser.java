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
            String fallbackSuggestion,
            Float minArea,
            Float maxArea,
            Integer bedrooms
    ) {
        public boolean hasStructuredFilter() {
            return productType != null || listingType != null
                    || minPrice != null || maxPrice != null
                    || minArea != null || maxArea != null || bedrooms != null
                    || (locationText != null && !locationText.isBlank())
                    || !amenities.isEmpty();
        }

        public Map<String, Object> toAppliedFilters() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (productType != null)   m.put("productType", productType);
            if (listingType != null)   m.put("listingType", listingType);
            if (minPrice != null)      m.put("minPrice", minPrice);
            if (maxPrice != null)      m.put("maxPrice", maxPrice);
            if (minArea != null)       m.put("minArea", minArea);
            if (maxArea != null)       m.put("maxArea", maxArea);
            if (bedrooms != null)      m.put("bedrooms", bedrooms);
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
        AMENITIES.put("internet",       "wifi");
        AMENITIES.put("cho de xe",      "chỗ để xe");
        AMENITIES.put("cho dau xe",     "chỗ để xe");
        AMENITIES.put("bai do xe",      "chỗ để xe");
        AMENITIES.put("gui xe",         "chỗ để xe");
        AMENITIES.put("ham xe",         "chỗ để xe");
        AMENITIES.put("bao ve",         "bảo vệ");
        AMENITIES.put("an ninh",        "bảo vệ");
        AMENITIES.put("nha bep",        "nhà bếp");
        AMENITIES.put("bep",            "nhà bếp");
        AMENITIES.put("ve sinh rieng",  "vệ sinh riêng");
        AMENITIES.put("khep kin",       "vệ sinh riêng");
        AMENITIES.put("phong khep kin", "phòng khép kín");
        AMENITIES.put("gio giac tu do", "giờ giấc tự do");
        AMENITIES.put("camera",         "camera an ninh");
        AMENITIES.put("cctv",           "camera an ninh");
        AMENITIES.put("ho boi",         "hồ bơi");
        AMENITIES.put("be boi",         "hồ bơi");
        AMENITIES.put("phong gym",      "phòng gym");
        AMENITIES.put("gym",            "phòng gym");
        AMENITIES.put("san vuon",       "sân vườn");
        AMENITIES.put("tivi",           "tivi");
        AMENITIES.put("giuong",         "giường");
        AMENITIES.put("nuoc nong",      "bình nước nóng");
        AMENITIES.put("binh nuoc nong", "bình nước nóng");
        AMENITIES.put("may nuoc nong",  "bình nước nóng");
        AMENITIES.put("thu cung",       "thú cưng");
        AMENITIES.put("nuoi pet",       "thú cưng");
    }

    /** Tokens that are never part of a location and carry no filter on their own. */
    private static final Set<String> STOPWORDS = Set.of(
            "gan", "o", "tai", "khu", "vuc", "vung", "quanh", "khu vuc",
            "gia", "re", "gia re", "dep", "moi", "rong", "rai", "co", "va",
            "can", "tim", "thue", "muon", "mua", "phong", "nha", "cho",
            "duoi", "tren", "tu", "den", "khoang", "tam", "gia tu",
            "khong", "qua", "toi", "da", "it", "nhat", "duoi muc",
            "dien", "tich", "dien tich", "m2", "met", "vuong", "mv",
            "ngu", "pn", "phong ngu", "lon", "rong tren", "rong hon",
            "nho hon", "lon hon"
    );

    private static final Pattern RANGE_PATTERN =
            Pattern.compile("(?:tu\\s+)?(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)?\\s*(?:-|den|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)");
    // TextNormalizer turns every non-alphanumeric char (incl. "-", "~", "→")
    // into a space, so "từ 10-20tr" arrives here as "tu 10 20 trieu" with no
    // separator left. A range cue ("tu/tam/khoang/gia") followed by two
    // numbers is therefore still a range even though the dash is gone.
    private static final Pattern RANGE_CUE_PATTERN =
            Pattern.compile("(?:tu|tam|khoang|gia)\\s+(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)?\\s+(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)");
    // "10tr-20tr" / "10 triệu 20 triệu": both bounds carry an explicit unit
    // and sit next to each other → a range even without a cue word.
    private static final Pattern RANGE_DUAL_UNIT_PATTERN =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)\\s+(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)");
    private static final Pattern MAX_PATTERN =
            Pattern.compile("(?:duoi|khong qua|toi da|<=|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)?");
    private static final Pattern MIN_PATTERN =
            Pattern.compile("(?:tren|tu|it nhat|>=|>)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)?");
    private static final Pattern LONE_PRICE_PATTERN =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|ty|nghin|ngan|k|cu)");

    // ── Area (m²) — always carries the explicit "m2" unit so it is extracted
    //    BEFORE price; otherwise "duoi 30m2" would be read as maxPrice 30 triệu.
    private static final Pattern AREA_RANGE_PATTERN =
            Pattern.compile("(?:dien tich\\s+)?(?:tu\\s+)?(\\d+(?:[.,]\\d+)?)\\s*(?:m2)?\\s*(?:-|den|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*m2");
    private static final Pattern AREA_MAX_PATTERN =
            Pattern.compile("(?:duoi|nho hon|toi da|<=|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*m2");
    private static final Pattern AREA_MIN_PATTERN =
            Pattern.compile("(?:tren|tu|lon hon|rong tren|rong hon|>=|>)\\s*(\\d+(?:[.,]\\d+)?)\\s*m2");
    private static final Pattern AREA_LONE_PATTERN =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*m2");

    // ── Bedrooms — "pn" / "p ngu" are expanded to "phong ngu" first.
    private static final Pattern BEDROOM_PATTERN =
            Pattern.compile("(\\d+)\\s*phong ngu");

    /**
     * District-level administrative markers ("quận", "huyện", "thị xã").
     * The legacy address tables store named districts WITHOUT this prefix
     * (district_name "Quận Tân Bình", but district_short_name "Tân Bình" and
     * district_short_key "tanbinh"), so leaving the marker in the location
     * text makes "quận tân bình" fail to resolve to the Tân Bình district.
     *
     * <p>The {@code (?=[a-z])} lookahead strips the marker only when a NAMED
     * unit follows ("quan tan binh" → "tan binh"); a numbered district
     * ("quan 1") keeps its marker so it still matches the {@code quan1} key.
     */
    private static final Pattern DISTRICT_UNIT_PREFIX =
            Pattern.compile("\\b(thi xa|quan|huyen)\\s+(?=[a-z])");

    public static ParsedQuery parse(String rawQuery) {
        String norm = expandAbbreviations(TextNormalizer.normalize(rawQuery));
        if (norm == null || norm.isBlank()) {
            return new ParsedQuery(null, null, null, null, null, List.of(), null, null,
                    null, null, null);
        }

        String working = " " + norm + " ";

        // 1a. Bedrooms — "2 phong ngu" owns its number, so consume it before
        //     price so "tu 2 phong ngu" is not misread as minPrice 2 triệu.
        Integer[] bedroomsHolder = new Integer[1];
        working = extractBedrooms(working, bedroomsHolder);

        // 1b. Area — always unit-qualified ("m2"); consume before price so
        //     "duoi 30m2" is not misread as maxPrice 30 triệu.
        Float[] area = new Float[2]; // [min, max]
        working = extractArea(working, area);

        // 1c. Price — owns the remaining ambiguous tokens (numbers, "tr").
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
        // also emit it as residualKeyword (that would double-filter). Strip a
        // district marker ("quận"/"huyện") so a named district resolves
        // ("quan tan binh" → "tan binh"); done BEFORE stopword removal so a
        // numbered district ("quan 1") still keeps its marker.
        working = DISTRICT_UNIT_PREFIX.matcher(working).replaceAll(" ");
        String locationText = stripStopwords(working);
        String fallback = buildFallbackSuggestion(
                productDisplay, locationText, price[0], price[1], amenities,
                area[0], area[1], bedroomsHolder[0]);

        return new ParsedQuery(
                productType, listingType, price[0], price[1],
                locationText.isBlank() ? null : locationText,
                amenities, null, fallback,
                area[0], area[1], bedroomsHolder[0]);
    }

    private static String extractBedrooms(String working, Integer[] out) {
        Matcher m = BEDROOM_PATTERN.matcher(working);
        if (m.find()) {
            try {
                out[0] = Integer.valueOf(m.group(1));
            } catch (NumberFormatException ignored) {
                // leave bedrooms unset on overflow / garbage
            }
            working = working.replace(m.group(), " ");
        }
        // Strip any remaining bare "phong ngu" so it does not leak into location.
        return working.replace(" phong ngu ", " ");
    }

    private static String extractArea(String working, Float[] out) {
        Matcher range = AREA_RANGE_PATTERN.matcher(working);
        if (range.find()) {
            out[0] = area(range.group(1));
            out[1] = area(range.group(2));
            return working.replace(range.group(), " ");
        }

        Matcher max = AREA_MAX_PATTERN.matcher(working);
        if (max.find()) {
            out[1] = area(max.group(1));
            working = working.replace(max.group(), " ");
        }

        Matcher min = AREA_MIN_PATTERN.matcher(working);
        if (min.find()) {
            out[0] = area(min.group(1));
            working = working.replace(min.group(), " ");
        }

        if (out[0] == null && out[1] == null) {
            Matcher lone = AREA_LONE_PATTERN.matcher(working);
            if (lone.find()) {
                // bare "25m2" → treat as a lower bound ("at least ~"), which is
                // how users mean it (they want it no smaller than that).
                out[0] = area(lone.group(1));
                working = working.replace(lone.group(), " ");
            }
        }
        // Drop any leftover lone "m2" unit token.
        return working.replace(" m2 ", " ");
    }

    private static Float area(String number) {
        if (number == null) return null;
        try {
            return Float.valueOf(number.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
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
        r = r.replace(" mlanh ", " may lanh ");
        r = r.replace(" mgiat ", " may giat ");
        r = r.replace(" tlanh ", " tu lanh ");
        r = r.replace(" full nt ", " full noi that ");
        r = r.replace(" fullnt ", " full noi that ");
        r = r.replace(" nt ", " noi that ");
        r = r.replace(" dctd ", " gio giac tu do ");
        r = r.replace(" wc ", " ve sinh rieng ");
        r = r.replace(" nvs ", " ve sinh rieng ");
        for (int i = 1; i <= 12; i++) {
            r = r.replace(" q" + i + " ", " quan " + i + " ");
        }
        // Bedrooms: "2pn", "2 pn", "2 p ngu", "2 phong ngu" → "2 phong ngu".
        r = r.replaceAll("(\\d+)\\s*pn\\b", "$1 phong ngu");
        r = r.replaceAll("(\\d+)\\s*p\\s*ngu\\b", "$1 phong ngu");
        r = r.replaceAll("(\\d+)\\s*phong\\s*ngu\\b", "$1 phong ngu");
        // Area unit: "m2", "m²" (→ "m 2" after normalize), "met vuong", "mv".
        r = r.replaceAll("(\\d+(?:[.,]\\d+)?)\\s*m\\s*2\\b", "$1 m2");
        r = r.replaceAll("(\\d+(?:[.,]\\d+)?)\\s*(?:met vuong|met vng|mv)\\b", "$1 m2");
        r = r.replaceAll("(\\d+(?:[.,]\\d+)?)\\s*m2\\b", "$1 m2");
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

        // Separator-less range: "(tu) 10 20 trieu" — the dash/word that used
        // to join the bounds was stripped by normalization.
        Matcher cue = RANGE_CUE_PATTERN.matcher(working);
        if (cue.find()) {
            String unit = cue.group(4) != null ? cue.group(4) : cue.group(2);
            out[0] = money(cue.group(1), unit);
            out[1] = money(cue.group(3), unit);
            return working.replace(cue.group(), " ");
        }

        // Both bounds explicitly unit-qualified and adjacent: "10 trieu 20 trieu".
        Matcher dual = RANGE_DUAL_UNIT_PATTERN.matcher(working);
        if (dual.find()) {
            out[0] = money(dual.group(1), dual.group(2));
            out[1] = money(dual.group(3), dual.group(4));
            return working.replace(dual.group(), " ");
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
            BigDecimal minPrice, BigDecimal maxPrice, List<String> amenities,
            Float minArea, Float maxArea, Integer bedrooms) {

        Set<String> parts = new LinkedHashSet<>();
        if (productDisplay != null) parts.add(productDisplay);
        if (bedrooms != null) parts.add(bedrooms + " phòng ngủ");
        if (locationText != null && !locationText.isBlank()) parts.add(locationText);
        if (maxPrice != null && minPrice != null) {
            parts.add("từ " + humanPrice(minPrice) + " đến " + humanPrice(maxPrice));
        } else if (maxPrice != null) {
            parts.add("dưới " + humanPrice(maxPrice));
        } else if (minPrice != null) {
            parts.add("trên " + humanPrice(minPrice));
        }
        if (minArea != null && maxArea != null) {
            parts.add("từ " + humanArea(minArea) + " đến " + humanArea(maxArea));
        } else if (maxArea != null) {
            parts.add("dưới " + humanArea(maxArea));
        } else if (minArea != null) {
            parts.add("trên " + humanArea(minArea));
        }
        parts.addAll(amenities);
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private static String humanArea(Float v) {
        if (v == null) return "";
        return (v == Math.floor(v) ? String.valueOf(v.longValue()) : String.valueOf(v)) + "m²";
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