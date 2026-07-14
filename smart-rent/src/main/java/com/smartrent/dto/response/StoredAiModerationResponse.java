package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * The AI moderation result stored by the auto-moderation cronjob, replayed for
 * admins so the review UI can show the analysis without re-running it.
 *
 * <p>{@code verification} and {@code duplicateCheck} are deserialized from the
 * {@code listing_ai_moderation.ai_reason} JSON (written as
 * {@code {verification, duplicateCheck}}) and re-serialized with their own
 * field annotations — verification stays snake_case, duplicateCheck camelCase —
 * so the admin frontend consumes them with the exact types the on-demand
 * /verify and /check-duplicate endpoints already return.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Stored AI moderation result (verification + duplicate check) for a listing")
public class StoredAiModerationResponse {

    @Schema(description = "Stored AI verification analysis (null if not present)")
    AiListingVerificationResponse verification;

    @Schema(description = "Stored AI duplicate-check result (null if not present)")
    DuplicateCheckResponse duplicateCheck;

    @Schema(description = "Overall AI score persisted on the moderation record")
    Double aiScore;

    @Schema(description = "Moderation verification status (VERIFIED, REJECTED, UNDER_REVIEW, PENDING, ...)")
    String verificationStatus;

    @Schema(description = "When the moderation record was last updated (i.e. when the AI analyzed it)")
    LocalDateTime analyzedAt;
}
