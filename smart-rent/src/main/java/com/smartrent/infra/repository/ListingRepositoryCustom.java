package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.listing.cursor.ListingCursorSupport;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Custom listing queries that the derived/Specification repository can't express.
 */
public interface ListingRepositoryCustom {

    /**
     * Keyset (cursor) page: applies {@code spec} (filters + seek predicate),
     * orders by {@code keys}, and returns at most {@code limit} rows as a plain
     * List — NO {@code COUNT(*)} (unlike {@code findAll(spec, Pageable)}). That
     * is the whole point of cursor pagination: deep pages stay O(limit).
     */
    List<Listing> findByCursor(Specification<Listing> spec,
                               List<ListingCursorSupport.CursorKey> keys,
                               int limit);
}
