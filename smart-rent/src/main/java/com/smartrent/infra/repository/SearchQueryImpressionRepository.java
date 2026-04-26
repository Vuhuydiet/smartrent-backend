package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.SearchQueryImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SearchQueryImpression}.
 * Inserts are fire-and-forget (no custom query methods needed beyond basic save).
 */
@Repository
public interface SearchQueryImpressionRepository extends JpaRepository<SearchQueryImpression, Long> {
}
