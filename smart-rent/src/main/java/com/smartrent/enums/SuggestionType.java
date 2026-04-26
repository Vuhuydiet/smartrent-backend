package com.smartrent.enums;

/**
 * Discriminates the source / type of a single suggestion item returned by
 * {@code GET /v1/listings/search-suggestions}.
 *
 * <ul>
 *   <li>{@link #TITLE}        – suggestion derived from a matching listing title prefix.</li>
 *   <li>{@link #LOCATION}     – suggestion derived from a matching province / district / ward name.</li>
 *   <li>{@link #POPULAR_QUERY}– suggestion derived from historically popular search terms.</li>
 * </ul>
 */
public enum SuggestionType {
    TITLE,
    LOCATION,
    POPULAR_QUERY
}
