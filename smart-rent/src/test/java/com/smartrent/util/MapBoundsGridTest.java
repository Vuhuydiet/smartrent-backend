package com.smartrent.util;

import com.smartrent.dto.request.MapBoundsRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MapBoundsGrid} snaps a map-bounds viewport to a fixed, zoom-dependent
 * grid before it is used as the {@code listing.map} Redis cache key + geo query
 * bbox. The point is hit-rate: two users browsing the same area at the same zoom
 * (whose exact viewports never match to 4 decimals) must collapse to ONE cache
 * entry instead of each re-running the geo query. The snapped region must still
 * fully contain the requested one so the cached result is correct for it.
 */
class MapBoundsGridTest {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private MapBoundsRequest.MapBoundsRequestBuilder base() {
        return MapBoundsRequest.builder()
                .swLat(new BigDecimal("10.7900"))
                .swLng(new BigDecimal("106.7000"))
                .neLat(new BigDecimal("10.8000"))
                .neLng(new BigDecimal("106.7100"))
                .zoom(14)
                .limit(200)
                .verifiedOnly(true)
                .categoryId(3L)
                .vipType("GOLD");
    }

    private static BigDecimal midpoint(BigDecimal a, BigDecimal b) {
        return a.add(b).divide(TWO, 12, RoundingMode.HALF_UP);
    }

    /** Correctness: the snapped region must never be smaller than what was asked. */
    @Test
    void snap_expandsRegionToContainOriginal() {
        MapBoundsRequest r = base().build();

        MapBoundsRequest s = MapBoundsGrid.snap(r);

        assertTrue(s.getSwLat().compareTo(r.getSwLat()) <= 0, "snapped SW lat must floor below request");
        assertTrue(s.getSwLng().compareTo(r.getSwLng()) <= 0, "snapped SW lng must floor below request");
        assertTrue(s.getNeLat().compareTo(r.getNeLat()) >= 0, "snapped NE lat must ceil above request");
        assertTrue(s.getNeLng().compareTo(r.getNeLng()) >= 0, "snapped NE lng must ceil above request");
    }

    /**
     * The core hit-rate behavior: a second viewport that pans within the same
     * grid cell (distinct raw coordinates) snaps to the SAME cache key. Built by
     * nudging each corner to the midpoint between the request and its own snapped
     * grid line — provably still inside the same cell — so the test is robust to
     * where the grid lines actually fall.
     */
    @Test
    void snap_collapsesSubCellPanToSameCacheKey() {
        MapBoundsRequest a = base().build();
        MapBoundsRequest snappedA = MapBoundsGrid.snap(a);

        MapBoundsRequest b = base()
                .swLat(midpoint(snappedA.getSwLat(), a.getSwLat()))
                .swLng(midpoint(snappedA.getSwLng(), a.getSwLng()))
                .neLat(midpoint(a.getNeLat(), snappedA.getNeLat()))
                .neLng(midpoint(a.getNeLng(), snappedA.getNeLng()))
                .build();

        // Precondition: a and b are genuinely different raw viewports.
        assertNotEquals(CacheKeyBuilder.mapBoundsKey(a), CacheKeyBuilder.mapBoundsKey(b));

        // ...yet after snapping they share one cache entry.
        assertEquals(
                CacheKeyBuilder.mapBoundsKey(MapBoundsGrid.snap(a)),
                CacheKeyBuilder.mapBoundsKey(MapBoundsGrid.snap(b)));
    }

    /**
     * The grid must tighten as zoom increases, otherwise zoomed-in viewports
     * would over-expand (querying far more than the user sees) and the cap could
     * drop pins actually in view. Same raw box → smaller snapped span at higher
     * zoom.
     */
    @Test
    void snap_gridIsFinerAtHigherZoom() {
        MapBoundsRequest coarse = base().zoom(10).build();
        MapBoundsRequest fine = base().zoom(18).build();

        BigDecimal coarseSpan = spanLng(MapBoundsGrid.snap(coarse));
        BigDecimal fineSpan = spanLng(MapBoundsGrid.snap(fine));

        assertTrue(fineSpan.compareTo(coarseSpan) < 0,
                "higher zoom must snap to a tighter grid (smaller span): fine=" + fineSpan + " coarse=" + coarseSpan);
    }

    private static BigDecimal spanLng(MapBoundsRequest r) {
        return r.getNeLng().subtract(r.getSwLng());
    }

    /** Snapping only touches geometry; every filter/limit/zoom field is carried through. */
    @Test
    void snap_preservesNonGeoFields() {
        MapBoundsRequest r = base().build();

        MapBoundsRequest s = MapBoundsGrid.snap(r);

        assertEquals(r.getZoom(), s.getZoom());
        assertEquals(r.getLimit(), s.getLimit());
        assertEquals(r.getVerifiedOnly(), s.getVerifiedOnly());
        assertEquals(r.getCategoryId(), s.getCategoryId());
        assertEquals(r.getVipType(), s.getVipType());
    }

    /** Defensive: a request without zoom can't be gridded, so it passes through untouched. */
    @Test
    void snap_withoutZoom_returnsUnchanged() {
        MapBoundsRequest r = base().zoom(null).build();

        MapBoundsRequest s = MapBoundsGrid.snap(r);

        assertEquals(r.getSwLat(), s.getSwLat());
        assertEquals(r.getNeLng(), s.getNeLng());
    }
}
