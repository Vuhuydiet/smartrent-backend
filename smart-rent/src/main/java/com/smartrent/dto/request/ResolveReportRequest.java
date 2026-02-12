package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to resolve or reject a listing report")
public class ResolveReportRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Resolution status", example = "RESOLVED",
            allowableValues = {"RESOLVED", "REJECTED"}, required = true)
    String status;

    @Schema(description = "Admin notes/comments on the resolution",
            example = "Please update listing title and real area")
    String adminNotes;

    // ── Owner action fields ──
    @Schema(description = "Whether the listing owner must take action", example = "true")
    Boolean ownerActionRequired;

    @Schema(description = "Type of action required from owner", example = "UPDATE_LISTING",
            allowableValues = {"UPDATE_LISTING", "CONTACT_SUPPORT"})
    String ownerActionType;

    @Schema(description = "Optional deadline for owner action")
    LocalDateTime ownerActionDeadlineAt;

    @Schema(description = "Whether to hide listing until owner fixes it", example = "KEEP_VISIBLE",
            allowableValues = {"KEEP_VISIBLE", "HIDE_UNTIL_REVIEW"})
    String listingVisibilityAction;
}

