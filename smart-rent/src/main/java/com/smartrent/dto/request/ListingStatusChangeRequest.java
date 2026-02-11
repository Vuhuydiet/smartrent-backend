package com.smartrent.dto.request;

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
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to change listing verification/moderation status")
public class ListingStatusChangeRequest {

    // ── Legacy fields (backward compat) ──
    @Schema(description = "[DEPRECATED] Use 'decision' instead. Legacy verification flag.",
            example = "true")
    Boolean verified;

    @Schema(description = "[DEPRECATED] Use 'reasonText' instead. Legacy reason field.",
            example = "Listing meets all verification requirements")
    String reason;

    // ── New moderation fields ──
    @Schema(description = "Moderation decision", example = "APPROVE",
            allowableValues = {"APPROVE", "REJECT", "REQUEST_REVISION"})
    String decision;

    @Schema(description = "Structured reason code for rejection/revision",
            example = "MISSING_INFO")
    String reasonCode;

    @Schema(description = "Human-readable reason for rejection/revision",
            example = "Missing legal information in description")
    String reasonText;

    @Schema(description = "Whether owner must take action", example = "true")
    Boolean ownerActionRequired;

    @Schema(description = "Optional deadline for owner action")
    LocalDateTime ownerActionDeadlineAt;

    /**
     * Returns true if the new decision-based fields are populated.
     */
    public boolean isNewFormat() {
        return decision != null && !decision.isBlank();
    }
}
