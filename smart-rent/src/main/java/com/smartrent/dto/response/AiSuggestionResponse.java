package com.smartrent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionResponse {
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    private String normalizedQuery;

    /**
     * Backend-ready filters the AI server resolved from the raw query using
     * its RAG knowledge base (location → legacy ids, amenity → ids, type →
     * enum, price/area). Forwarded into {@code SearchSuggestionsResponse
     * .appliedFilters} so the frontend applies structured filters instead of
     * FULLTEXT-searching the raw query. Null when nothing structured parsed.
     */
    private Map<String, Object> appliedFilters;
}
