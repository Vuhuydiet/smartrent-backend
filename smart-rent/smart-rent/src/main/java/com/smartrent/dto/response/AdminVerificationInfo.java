package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Admin verification information for listing")
public class AdminVerificationInfo {

    @Schema(description = "Admin ID who verified the listing", example = "admin-123e4567-e89b-12d3-a456-426614174000")
    String adminId;

    @Schema(description = "Admin's full name", example = "Jane Smith")
    String adminName;

    @Schema(description = "Admin's email", example = "admin@smartrent.com")
    String adminEmail;

    @Schema(description = "Verification timestamp", example = "2025-09-01T10:00:00")
    LocalDateTime verifiedAt;

    @Schema(description = "Verification status", example = "APPROVED", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
    String verificationStatus;

    @Schema(description = "Admin's notes or comments on verification", example = "Verified property documents and ownership")
    String verificationNotes;
}