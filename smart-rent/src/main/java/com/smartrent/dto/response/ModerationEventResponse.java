package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Timeline entry representing a single moderation state transition.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "A moderation event in the listing's audit trail")
public class ModerationEventResponse {

    @Schema(description = "Event ID", example = "1")
    Long eventId;

    @Schema(description = "Source of the moderation action", example = "NEW_SUBMISSION",
            allowableValues = {"NEW_SUBMISSION", "REPORT_RESOLUTION", "OWNER_EDIT"})
    String source;

    @Schema(description = "Previous moderation status", example = "PENDING_REVIEW")
    String fromStatus;

    @Schema(description = "New moderation status", example = "REJECTED")
    String toStatus;

    @Schema(description = "Action performed", example = "REJECT",
            allowableValues = {"APPROVE", "REJECT", "REQUEST_REVISION", "RESUBMIT", "SUSPEND"})
    String action;

    @Schema(description = "Structured reason code", example = "MISSING_INFO")
    String reasonCode;

    @Schema(description = "Human-readable reason", example = "Missing legal information in description")
    String reasonText;

    @Schema(description = "Admin ID who performed the action")
    String adminId;

    @Schema(description = "Admin name (if available)")
    String adminName;

    @Schema(description = "User ID who triggered (for resubmit)")
    String triggeredByUserId;

    @Schema(description = "Linked report ID")
    Long reportId;

    @Schema(description = "When this event occurred")
    LocalDateTime createdAt;
}
