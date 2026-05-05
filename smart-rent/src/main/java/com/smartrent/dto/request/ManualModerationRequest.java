package com.smartrent.dto.request;

import com.smartrent.enums.ModerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request for manual moderation by an administrator")
public class ManualModerationRequest {

    @NotNull(message = "Action is required")
    @Schema(description = "Moderation action (APPROVED, REJECTED, REVISION_REQUIRED)", example = "APPROVED")
    ModerationStatus action;

    @Schema(description = "Optional reason for the moderation decision", example = "Property details verified via phone call")
    String reason;

    @Schema(description = "Internal code for the reason", example = "MANUAL_VERIFICATION")
    String reasonCode;
}
