package com.smartrent.util;

import com.smartrent.dto.request.MapBoundsRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Snaps a map-bounds viewport to a fixed, zoom-dependent grid so overlapping
 * viewports collapse to a single {@code listing.map} cache key.
 *
 * <p>The frontend rounds the raw bbox to 4 decimals (~11m) before sending, but
 * that key still depends on each user's exact map centre (their GPS location)
 * and screen pixel span, so two people browsing the same area almost never share
 * a key — the cross-user hit rate is ~0 and every first load re-runs the geo
 * query. Snapping each corner to a global grid whose cell size is a function of
 * <em>zoom only</em> makes all viewers at the same zoom land on the same grid
 * lines, so their viewports share one cached entry.
 *
 * <p>SW floors down and NE ceils up, so the snapped cell always <b>contains</b>
 * the requested viewport: the cached result is a superset and the frontend
 * filters client-side to the true viewport, so nothing in view is ever missing.
 * The step halves each zoom level (Web-Mercator tiles do the same), so a
 * zoomed-in viewport is never expanded by more than a fraction of itself.
 */
public final class MapBoundsGrid {

    private MapBoundsGrid() {}

    // Scale kept on the computed step + snapped corners. 12 dp is far finer than
    // the ~11m the frontend already quantizes to, and — crucially — is fixed, so
    // the same (zoom, cell) always yields the same BigDecimal and therefore the
    // same cache-key string.
    private static final int GRID_SCALE = 12;
    private static final BigDecimal FULL_LNG_SPAN = BigDecimal.valueOf(360);

    /**
     * @return a copy of {@code request} with its bbox snapped to the zoom grid;
     *         the same instance untouched when it carries no zoom (nothing to
     *         grid against).
     */
    public static MapBoundsRequest snap(MapBoundsRequest request) {
        if (request == null || request.getZoom() == null) {
            return request;
        }

        BigDecimal step = stepForZoom(request.getZoom());

        return MapBoundsRequest.builder()
                .swLat(floorToStep(request.getSwLat(), step))
                .swLng(floorToStep(request.getSwLng(), step))
                .neLat(ceilToStep(request.getNeLat(), step))
                .neLng(ceilToStep(request.getNeLng(), step))
                .zoom(request.getZoom())
                .limit(request.getLimit())
                .verifiedOnly(request.getVerifiedOnly())
                .categoryId(request.getCategoryId())
                .vipType(request.getVipType())
                .build();
    }

    // Grid cell size in degrees = 360 / 2^(zoom+2), i.e. a quarter of a
    // Web-Mercator tile's longitude span at this zoom. Same latitude step is
    // reused: at Vietnam's latitudes a degree of lat and lng are close enough
    // that a shared step keeps cells roughly square, and it keeps the grid math
    // trivial.
    private static BigDecimal stepForZoom(int zoom) {
        BigDecimal divisor = BigDecimal.valueOf(2).pow(zoom + 2);
        return FULL_LNG_SPAN.divide(divisor, GRID_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal floorToStep(BigDecimal value, BigDecimal step) {
        if (value == null) {
            return null;
        }
        return value.divide(step, 0, RoundingMode.FLOOR).multiply(step);
    }

    private static BigDecimal ceilToStep(BigDecimal value, BigDecimal step) {
        if (value == null) {
            return null;
        }
        return value.divide(step, 0, RoundingMode.CEILING).multiply(step);
    }
}
