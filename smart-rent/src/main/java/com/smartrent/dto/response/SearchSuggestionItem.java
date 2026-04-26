package com.smartrent.dto.response;

import com.smartrent.enums.SuggestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Map;

/**
 * A single suggestion item returned by {@code GET /v1/listings/search-suggestions}.
 *
 * <p>The {@code score} field is a floating-point relevance value used to rank suggestions
 * (higher = more relevant). It is provided for transparency and may be used by the
 * frontend to render badges (e.g. "popular", "exact match").
 */
@Schema(description = "A single search suggestion item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchSuggestionItem {

    @Schema(
        description = "Source type of the suggestion",
        example = "TITLE",
        allowableValues = {"TITLE", "LOCATION", "POPULAR_QUERY"}
    )
    SuggestionType type;

    @Schema(
        description = "Display text for the suggestion (original, non-normalized)",
        example = "Căn hộ 2PN Quận 1 full nội thất"
    )
    String text;

    @Schema(
        description = "Listing ID — populated only when type = TITLE, null otherwise",
        example = "12345",
        nullable = true
    )
    Long listingId;

    @Schema(
        description = "Optional metadata map (e.g. province name, ward name, hit count). " +
                      "Content varies by suggestion type.",
        nullable = true,
        example = "{\"provinceName\": \"TP. Hồ Chí Minh\", \"districtName\": \"Quận 1\"}"
    )
    Map<String, Object> metadata;

    @Schema(
        description = "Weighted relevance score (higher = more relevant). " +
                      "Suggestions are returned pre-sorted descending by this value.",
        example = "1.45"
    )
    double score;
}
