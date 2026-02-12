package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response DTO for a pending owner action (obligation to update listing).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "A pending action the owner must complete")
public class OwnerActionResponse {

    @Schema(description = "Owner action ID", example = "1")
    Long ownerActionId;

    @Schema(description = "Listing ID", example = "123")
    Long listingId;

    @Schema(description = "What triggered this action", example = "LISTING_REJECTED",
            allowableValues = {"REPORT_RESOLVED", "LISTING_REJECTED"})
    String triggerType;

    @Schema(description = "Reference ID of the trigger (report ID or event ID)")
    Long triggerRefId;

    @Schema(description = "What the owner needs to do", example = "UPDATE_LISTING",
            allowableValues = {"UPDATE_LISTING", "CONTACT_SUPPORT"})
    String requiredAction;

    @Schema(description = "Current status", example = "PENDING_OWNER",
            allowableValues = {"PENDING_OWNER", "OWNER_UPDATED", "SUBMITTED_FOR_REVIEW", "COMPLETED", "EXPIRED"})
    String status;

    @Schema(description = "Deadline for the owner to act")
    LocalDateTime deadlineAt;

    @Schema(description = "When the action was completed")
    LocalDateTime completedAt;

    @Schema(description = "When this action was created")
    LocalDateTime createdAt;
}
