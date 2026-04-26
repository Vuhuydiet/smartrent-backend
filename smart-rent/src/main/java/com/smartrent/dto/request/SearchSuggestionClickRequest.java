package com.smartrent.dto.request;

import com.smartrent.enums.SuggestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Request body for {@code POST /v1/listings/search-suggestions/click}.
 * Records that the user selected a specific suggestion from the result set identified
 * by {@code impressionId}.
 */
@Schema(description = "Click event payload for a search suggestion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchSuggestionClickRequest {

    @Schema(
        description = "Impression ID returned in the original search-suggestions response. " +
                      "Set to 0 if not available (e.g. clicked from cache without a fresh call).",
        example = "9876"
    )
    long impressionId;

    @NotNull(message = "Suggestion type must not be null")
    @Schema(description = "Type of the clicked suggestion", example = "TITLE",
            allowableValues = {"TITLE", "LOCATION", "POPULAR_QUERY"})
    SuggestionType type;

    @NotBlank(message = "Suggestion text must not be blank")
    @Schema(description = "Display text of the clicked suggestion",
            example = "Căn hộ 2PN Quận 1 full nội thất")
    String text;

    @Schema(description = "Listing ID for TITLE suggestions, null otherwise",
            example = "12345", nullable = true)
    Long listingId;

    @Min(value = 0, message = "Rank position must be >= 0")
    @Schema(description = "0-indexed position of this suggestion in the returned list",
            example = "2")
    int rank;
}
