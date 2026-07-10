package com.smartrent.service.listing;

import com.smartrent.dto.response.AdminListingListResponse;
import com.smartrent.infra.repository.ListingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dashboard statistics for {@code POST /v1/listings/admin/list}.
 *
 * <p>{@code getStatistics()} aggregates over the ENTIRE listings table
 * (no WHERE clause) — every admin list request was paying that full scan on
 * top of the paginated query, regardless of page/filters. Cached with a
 * short TTL (see application.yml) so repeated admin requests within the
 * window reuse one computed result instead of re-scanning the table each
 * time. Lives in its own bean (not a private method on ListingServiceImpl)
 * because {@code @Cacheable} can't intercept self-invoked calls within the
 * same class.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminListingStatsService {

    ListingRepository listingRepository;

    @Cacheable(cacheNames = com.smartrent.config.Constants.CacheNames.LISTING_STATS_ADMIN)
    public AdminListingListResponse.AdminStatistics getStatistics() {
        List<Object[]> results = listingRepository.getAdminStatistics();
        if (results == null || results.isEmpty()) {
            return AdminListingListResponse.AdminStatistics.builder()
                    .pendingVerification(0L).verified(0L).expired(0L).rejected(0L)
                    .drafts(0L).shadows(0L)
                    .normalListings(0L).silverListings(0L).goldListings(0L).diamondListings(0L)
                    .totalListings(0L)
                    .build();
        }
        Object[] row = results.get(0);
        return AdminListingListResponse.AdminStatistics.builder()
                .pendingVerification(toLong(row[0]))
                .verified(toLong(row[1]))
                .expired(toLong(row[2]))
                .rejected(toLong(row[3]))
                .drafts(toLong(row[4]))
                .shadows(toLong(row[5]))
                .normalListings(toLong(row[6]))
                .silverListings(toLong(row[7]))
                .goldListings(toLong(row[8]))
                .diamondListings(toLong(row[9]))
                .totalListings(toLong(row[10]))
                .build();
    }

    private static Long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }
}
