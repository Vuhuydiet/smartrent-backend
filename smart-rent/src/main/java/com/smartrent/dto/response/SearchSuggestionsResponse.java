package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Top-level response envelope for {@code GET /v1/listings/search-suggestions}.
 *
 * <ul>
 *   <li>{@code suggestions} — ordered list of ranked suggestion items (max {@code limit}).</li>
 *   <li>{@code queryNorm}   — normalized form of the raw input query (useful for cache debugging).</li>
 *   <li>{@code impressionId} — telemetry handle; pass this back in the click-tracking request
 *                              ({@code POST /v1/listings/search-suggestions/click}) so the backend
 *                              can correlate the click to this specific impression.</li>
 * </ul>
 */
@Schema(description = "Response envelope for the search suggestions endpoint")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchSuggestionsResponse {

    @Schema(description = "Ranked list of suggestions (max = limit param, default 8)")
    List<SearchSuggestionItem> suggestions;

    @Schema(
        description = "Normalized form of the raw query (useful for debugging caching behaviour)",
        example = "can ho quan 1"
    )
    String queryNorm;

    @Schema(
        description = "Telemetry impression ID. Pass back in the click event request to link " +
                      "the click to this result set.",
        example = "9876"
    )
    long impressionId;
}
