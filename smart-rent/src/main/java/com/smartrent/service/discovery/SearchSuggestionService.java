package com.smartrent.service.discovery;

import com.smartrent.dto.response.SearchSuggestionsResponse;
import com.smartrent.enums.SuggestionType;

/**
 * Service for generating smart, multi-source search suggestions.
 *
 * <p>Sources (merged and ranked by weighted score):
 * <ol>
 *   <li><b>TITLE</b>        — listing title-prefix matches from the {@code listings} table.</li>
 *   <li><b>LOCATION</b>     — province / district / ward name prefix matches from the address tables.</li>
 *   <li><b>POPULAR_QUERY</b>— historically clicked query terms from the telemetry table.</li>
 * </ol>
 *
 * <p>The existing {@code GET /v1/listings/autocomplete} endpoint is <em>not</em> affected.
 */
public interface SearchSuggestionService {

    /**
     * Build and return a merged, ranked list of suggestions for the given query.
     *
     * <p>Returns an empty {@code suggestions} list (never {@code null}) when the
     * normalized query is shorter than 2 characters.
     *
     * @param query      Raw input query string (will be normalized internally)
     * @param limit      Maximum number of suggestions to return; clamped to [1, 20]
     * @param provinceId Optional legacy province ID string (numeric) for scoped filtering
     * @param categoryId Optional category ID for scoped filtering
     * @param clientIp   Best-effort client IP for telemetry (may be {@code null})
     * @param sessionId  Optional opaque session token for correlating impressions with clicks
     * @return Populated response envelope including suggestions and the telemetry impression ID
     */
    SearchSuggestionsResponse getSuggestions(
        String query,
        int    limit,
        String provinceId,
        Long   categoryId,
        String clientIp,
        String sessionId
    );

    /**
     * Record a user click on a suggestion (fire-and-forget telemetry).
     *
     * <p>This method persists a {@link com.smartrent.infra.repository.entity.SearchSuggestionClick}
     * in a dedicated transaction that is independent of any surrounding transaction so that
     * click recording never blocks or rolls back due to other failures.
     *
     * @param impressionId Impression ID from the original suggestions response (0 if unavailable)
     * @param type         Type of the clicked suggestion
     * @param text         Display text of the clicked suggestion
     * @param listingId    Listing ID for TITLE suggestions; {@code null} for others
     * @param rank         0-indexed position of the suggestion in the result list
     */
    void recordClick(
        long           impressionId,
        SuggestionType type,
        String         text,
        Long           listingId,
        int            rank
    );
}
