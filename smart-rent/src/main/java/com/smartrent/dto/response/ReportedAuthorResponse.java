package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
 * Admin view of a listing author (người đăng tin) together with their report counts
 * and current posting-block state.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "A reported listing author with report counts and block state")
public class ReportedAuthorResponse {

    @Schema(description = "Author's user ID", example = "user-123e4567-e89b-12d3-a456-426614174000")
    String userId;

    @Schema(description = "Author's first name", example = "Nguyen")
    String firstName;

    @Schema(description = "Author's last name", example = "Van A")
    String lastName;

    @Schema(description = "Author's email", example = "nguyen.vana@example.com")
    String email;

    @Schema(description = "Author's phone number", example = "0912345678")
    String phoneNumber;

    @Schema(description = "Author's avatar URL")
    String avatarUrl;

    @Schema(description = "Total reports across all of the author's listings", example = "7")
    Long totalReports;

    @Schema(description = "Reports approved (RESOLVED) by an admin", example = "4")
    Long resolvedReports;

    @Schema(description = "Whether the author has enough approved reports (> 3) to be blocked", example = "true")
    Boolean blockEligible;

    @Schema(description = "Whether the author is currently blocked from posting", example = "false")
    Boolean postingBlocked;

    @Schema(description = "Reason for the posting block (present when blocked)")
    String postingBlockedReason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "When the posting block was applied")
    LocalDateTime postingBlockedAt;
}
