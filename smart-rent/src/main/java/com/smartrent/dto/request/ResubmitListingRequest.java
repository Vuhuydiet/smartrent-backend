package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Optional request body for owner resubmit action.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to resubmit a listing for review after making required changes")
public class ResubmitListingRequest {

    @Schema(description = "Optional notes from the owner describing what was changed",
            example = "Updated title and added missing legal information")
    String notes;
}
