package com.smartrent.util;

import com.smartrent.dto.request.CategoryStatsRequest;
import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ProvinceStatsRequest;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class CacheKeyBuilder {
    private CacheKeyBuilder() {}

    public static String listingSearchKey(ListingFilterRequest filter) {
        if (filter == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(256);
        sb.append("v2|");

        append(sb, "userId", filter.getUserId());
        append(sb, "isDraft", filter.getIsDraft());
        append(sb, "verified", filter.getVerified());
        append(sb, "isVerify", filter.getIsVerify());
        append(sb, "expired", filter.getExpired());
        append(sb, "excludeExpired", filter.getExcludeExpired());
        append(sb, "status", filter.getStatus());
        append(sb, "listingStatus", filter.getListingStatus());
        append(sb, "isAdminRequest", filter.getIsAdminRequest());

        append(sb, "provinceId", filter.getProvinceId());
        append(sb, "provinceCode", filter.getProvinceCode());
        append(sb, "provinceCodes", filter.getProvinceCodes());
        append(sb, "districtId", filter.getDistrictId());
        append(sb, "wardId", filter.getWardId());
        append(sb, "newWardCode", filter.getNewWardCode());
        append(sb, "streetId", filter.getStreetId());
        append(sb, "isLegacy", filter.getIsLegacy());
        append(sb, "latitude", filter.getLatitude());
        append(sb, "longitude", filter.getLongitude());
        // NOTE: userLatitude/userLongitude/radiusKm are intentionally NOT part of
        // the key. The search query (ListingSpecification + sort) ignores them
        // entirely — two requests differing only in user coordinates produce an
        // identical result — so including them only multiplied the key space by
        // every distinct GPS fix and drove the cache hit-rate to ~0 (each user,
        // and each GPS jitter, minted a brand-new key for the same result).
        // If geo-proximity ranking is ever implemented, re-add these AND bucket
        // the coordinates (e.g. round to ~2 decimals) so the cache still groups.

        append(sb, "categoryId", filter.getCategoryId());
        append(sb, "listingType", filter.getListingType());
        append(sb, "vipType", filter.getVipType());
        append(sb, "productType", filter.getProductType());

        append(sb, "price", filter.getPrice());
        append(sb, "priceUnit", filter.getPriceUnit());
        append(sb, "hasPriceReduction", filter.getHasPriceReduction());
        append(sb, "hasPriceIncrease", filter.getHasPriceIncrease());
        append(sb, "priceReductionPercent", filter.getPriceReductionPercent());
        append(sb, "priceChangedWithinDays", filter.getPriceChangedWithinDays());

        append(sb, "area", filter.getArea());
        append(sb, "bedrooms", filter.getBedrooms());
        append(sb, "bathrooms", filter.getBathrooms());
        append(sb, "bedroomsRange", filter.getBedroomsRange());
        append(sb, "bathroomsRange", filter.getBathroomsRange());
        append(sb, "furnishing", filter.getFurnishing());
        append(sb, "direction", filter.getDirection());
        append(sb, "roomCapacity", filter.getRoomCapacity());

        append(sb, "waterPrice", filter.getWaterPrice());
        append(sb, "electricityPrice", filter.getElectricityPrice());
        append(sb, "internetPrice", filter.getInternetPrice());
        append(sb, "serviceFee", filter.getServiceFee());

        append(sb, "amenityIds", sortAndJoin(filter.getAmenityIds()));
        append(sb, "amenityMatchMode", filter.getAmenityMatchMode());
        append(sb, "hasMedia", filter.getHasMedia());
        append(sb, "minMediaCount", filter.getMinMediaCount());

        append(sb, "keyword", TextNormalizer.normalize(filter.getKeyword()));

        append(sb, "ownerPhoneVerified", filter.getOwnerPhoneVerified());
        append(sb, "isBroker", filter.getIsBroker());
        append(sb, "postedWithinDays", filter.getPostedWithinDays());
        append(sb, "updatedWithinDays", filter.getUpdatedWithinDays());

        append(sb, "page", filter.getPage());
        append(sb, "size", filter.getSize());
        append(sb, "sortBy", filter.getSortBy());
        append(sb, "sortDirection", filter.getSortDirection());

        return sb.toString();
    }

    private static void append(StringBuilder sb, String key, Object value) {
        sb.append(key).append('=').append(Objects.toString(value, "")).append('|');
    }

    private static String sortAndJoin(Set<Long> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .sorted()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    /**
     * Cache key for {@code GET /v1/listings/search-suggestions}.
     * Encodes: canonical query + provinceId + categoryId + limit.
     *
     * <p>The query part uses {@link SearchTextCanonicalizer#cacheCanonical}
     * (not just {@link TextNormalizer#normalize}) so abbreviation variants of
     * the SAME intent — {@code "q1"} / {@code "quan 1"}, {@code "5tr"} /
     * {@code "5 trieu"}, {@code "phongtro"} / {@code "phong tro"} — collapse to
     * a single entry instead of each re-running the full DB resolution (title
     * FULLTEXT + popular-query scan + parse). It only ever merges inputs that
     * provably produce an identical response, so the cached list is always
     * correct for the key.
     *
     * @param query      Raw input query (canonicalized internally)
     * @param provinceId Optional province ID string (may be {@code null})
     * @param categoryId Optional category ID (may be {@code null})
     * @param limit      Requested result count (already clamped by service)
     * @return Cache key string prefixed with {@code "sugg|"}
     */
    /**
     * Cache key for {@code POST /v1/listings/stats/categories}.
     * Encodes the sorted set of category ids plus the verifiedOnly toggle. The
     * homepage and a handful of admin views only ever ask for the full 5-category
     * set, so the key cardinality stays in the single digits.
     */
    public static String categoryStatsKey(CategoryStatsRequest request) {
        if (request == null) {
            return "null";
        }
        return "catStats|ids=" + sortAndJoinLongs(request.getCategoryIds())
             + "|verifiedOnly=" + Objects.toString(request.getVerifiedOnly(), "false");
    }

    /**
     * Cache key for {@code POST /v1/listings/stats/provinces}.
     * Encodes the sorted province ids + sorted province codes + addressType +
     * the verifiedOnly toggle. The homepage only ever asks for the same fixed
     * set of top provinces, so the key cardinality stays tiny.
     */
    public static String provinceStatsKey(ProvinceStatsRequest request) {
        if (request == null) {
            return "null";
        }
        return "provStats|ids=" + sortAndJoinIntegers(request.getProvinceIds())
             + "|codes=" + sortAndJoinStrings(request.getProvinceCodes())
             + "|addressType=" + Objects.toString(request.getAddressType(), "")
             + "|verifiedOnly=" + Objects.toString(request.getVerifiedOnly(), "false");
    }

    private static String sortAndJoinIntegers(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .filter(Objects::nonNull)
            .sorted()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    private static String sortAndJoinStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.joining(","));
    }

    private static String sortAndJoinLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .filter(Objects::nonNull)
            .sorted()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    /**
     * Cache key for {@code POST /v1/listings/map-bounds}.
     * Encodes the bounding box, zoom, limit and filters. The frontend already
     * rounds the bounds to 4 decimals (~11m) before sending, so identical
     * viewports — the same user returning to a spot, or many users browsing the
     * same area — collapse to a single entry instead of each re-running the geo
     * query.
     *
     * @param request Map bounds request (must not be {@code null} at call time —
     *                guarded for safety)
     * @return Cache key string prefixed with {@code "map|"}
     */
    public static String mapBoundsKey(com.smartrent.dto.request.MapBoundsRequest request) {
        if (request == null) {
            return "null";
        }
        return "map|ne=" + Objects.toString(request.getNeLat(), "") + ',' + Objects.toString(request.getNeLng(), "")
             + "|sw=" + Objects.toString(request.getSwLat(), "") + ',' + Objects.toString(request.getSwLng(), "")
             + "|z=" + Objects.toString(request.getZoom(), "")
             + "|lim=" + Objects.toString(request.getLimit(), "")
             + "|cat=" + Objects.toString(request.getCategoryId(), "")
             + "|vip=" + Objects.toString(request.getVipType(), "")
             + "|ver=" + Objects.toString(request.getVerifiedOnly(), "false");
    }

    public static String suggestionKey(String query, String provinceId, Long categoryId, int limit) {
        String norm = SearchTextCanonicalizer.cacheCanonical(query);
        return "sugg|q="  + Objects.toString(norm, "")
             + "|p="      + Objects.toString(provinceId, "")
             + "|c="      + Objects.toString(categoryId, "")
             + "|l="      + limit;
    }
}
