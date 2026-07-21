package com.smartrent.service.predictor.impl;

import com.smartrent.dto.request.PriceComparablesRequest;
import com.smartrent.dto.response.PriceComparablesResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.service.predictor.PriceComparablesService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PriceComparablesServiceImpl implements PriceComparablesService {

    static final double DEFAULT_RADIUS_KM = 2.0;
    static final double MAX_RADIUS_KM = 20.0;
    static final int DEFAULT_LIMIT = 100;
    static final int MAX_LIMIT = 200;
    // 1 degree of latitude ≈ 111.32 km everywhere; longitude shrinks by cos(lat).
    static final double KM_PER_DEGREE_LAT = 111.32;

    ListingRepository listingRepository;

    @Override
    public PriceComparablesResponse getComparables(PriceComparablesRequest request) {
        double lat = request.getLatitude();
        double lng = request.getLongitude();
        double radiusKm = clamp(
                request.getRadiusKm() != null ? request.getRadiusKm() : DEFAULT_RADIUS_KM,
                0.1, MAX_RADIUS_KM);
        int limit = request.getLimit() != null
                ? Math.min(Math.max(request.getLimit(), 1), MAX_LIMIT)
                : DEFAULT_LIMIT;
        String listingType = request.getListingType() != null ? request.getListingType() : "RENT";
        String priceUnit = request.getPriceUnit() != null ? request.getPriceUnit() : "MONTH";

        // Bounding box around the center so MySQL can range-scan the latitude
        // index column instead of evaluating Haversine over the whole table.
        double latDelta = radiusKm / KM_PER_DEGREE_LAT;
        double lngDelta = radiusKm / (KM_PER_DEGREE_LAT * Math.max(0.01, Math.cos(Math.toRadians(lat))));

        List<Object[]> rows = listingRepository.findPriceComparables(
                lat, lng,
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta,
                radiusKm,
                listingType,
                request.getProductType(),
                priceUnit,
                request.getMinArea(),
                request.getMaxArea(),
                limit);

        List<Long> prices = new ArrayList<>(rows.size());
        List<Long> pricePerSqm = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            if (row[0] == null) {
                continue;
            }
            long price = ((BigDecimal) row[0]).longValue();
            if (price <= 0) {
                continue;
            }
            prices.add(price);
            Float area = row[1] != null ? ((Number) row[1]).floatValue() : null;
            if (area != null && area > 0) {
                pricePerSqm.add(Math.round((double) price / area));
            }
        }

        if (prices.isEmpty()) {
            log.info("price-comparables: no matches for productType={} within {}km of ({},{})",
                    request.getProductType(), radiusKm, lat, lng);
            return PriceComparablesResponse.builder()
                    .sampleSize(0)
                    .currency("VND")
                    .build();
        }

        prices.sort(null);
        pricePerSqm.sort(null);

        return PriceComparablesResponse.builder()
                .sampleSize(prices.size())
                .min(prices.get(0))
                .p25(percentile(prices, 0.25))
                .median(percentile(prices, 0.50))
                .p75(percentile(prices, 0.75))
                .max(prices.get(prices.size() - 1))
                .avg(mean(prices))
                .pricePerSqmMedian(pricePerSqm.isEmpty() ? null : percentile(pricePerSqm, 0.50))
                .currency("VND")
                .build();
    }

    /** Linear-interpolated percentile over an already-sorted, non-empty list. */
    static Long percentile(List<Long> sorted, double q) {
        int n = sorted.size();
        if (n == 1) {
            return sorted.get(0);
        }
        double pos = q * (n - 1);
        int lower = (int) Math.floor(pos);
        int upper = (int) Math.ceil(pos);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double frac = pos - lower;
        return Math.round(sorted.get(lower) * (1 - frac) + sorted.get(upper) * frac);
    }

    static Long mean(List<Long> values) {
        long sum = 0;
        for (long v : values) {
            sum += v;
        }
        return Math.round((double) sum / values.size());
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
