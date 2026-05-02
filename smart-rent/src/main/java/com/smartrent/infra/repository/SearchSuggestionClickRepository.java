package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.SearchSuggestionClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for {@link SearchSuggestionClick}.
 *
 * <p>The {@link #findPopularQueryTexts} query drives the POPULAR_QUERY suggestion source:
 * it aggregates click events of type {@code POPULAR_QUERY} over a rolling time window
 * and returns the top-N clicked terms, providing a lightweight signals-based ranking
 * without requiring a separate analytics pipeline.
 */
@Repository
public interface SearchSuggestionClickRepository extends JpaRepository<SearchSuggestionClick, Long> {

    /**
     * Returns the most-clicked suggestion texts of type {@code POPULAR_QUERY}
     * within the given time window, ordered by click count descending.
     *
     * <p>This native query intentionally targets suggestion_type = 'POPULAR_QUERY' only
     * so that repeated listing-title clicks do not inflate "popular queries".
     *
     * @param since  Start of the rolling window (e.g. {@code now() - 7 days})
     * @param limit  Maximum number of popular terms to return
     * @return List of {@code [suggestion_text (String), hit_count (Long)]} raw tuples
     */
    @Query(nativeQuery = true, value = """
        SELECT   ssc.suggestion_text,
                 COUNT(*) AS hit_count
        FROM     search_suggestion_clicks ssc
        WHERE    ssc.suggestion_type = 'POPULAR_QUERY'
          AND    ssc.created_at     >= :since
        GROUP BY ssc.suggestion_text
        ORDER BY hit_count DESC, ssc.suggestion_text ASC
        LIMIT    :lim
        """)
    List<Object[]> findPopularQueryTexts(
        @Param("since") LocalDateTime since,
        @Param("lim")   int limit
    );
}
