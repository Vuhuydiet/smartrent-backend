package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Saved listing response containing the saved listing details and associated listing information")
public class SavedListingResponse {

    @Schema(
        description = "User ID who saved the listing",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    String userId;

    @Schema(
        description = "ID of the saved listing",
        example = "123"
    )
    Long listingId;

    @Schema(
        description = "Timestamp when the listing was saved",
        example = "2024-12-06T10:30:00"
    )
    LocalDateTime createdAt;

    @Schema(
        description = "Timestamp when the saved listing was last updated",
        example = "2024-12-06T10:30:00"
    )
    LocalDateTime updatedAt;

    @Schema(
        description = "Full listing details (optional - included when retrieving saved listings with details)",
        implementation = ListingResponse.class
    )
    ListingResponse listing;
}
