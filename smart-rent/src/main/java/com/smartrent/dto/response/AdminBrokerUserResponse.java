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
@Schema(description = "Admin view of a user's broker registration details")
public class AdminBrokerUserResponse {

    @Schema(description = "User ID", example = "user-123e4567-e89b-12d3-a456-426614174000")
    String userId;

    @Schema(description = "User's first name", example = "Nguyen")
    String firstName;

    @Schema(description = "User's last name", example = "Van A")
    String lastName;

    @Schema(description = "User's email address", example = "nguyen.vana@example.com")
    String email;

    @Schema(description = "International phone code", example = "+84")
    String phoneCode;

    @Schema(description = "Phone number", example = "0912345678")
    String phoneNumber;

    @Schema(description = "Whether this user is an approved broker", example = "false")
    Boolean isBroker;

    @Schema(
            description = "Current broker verification status",
            example = "PENDING",
            allowableValues = {"NONE", "PENDING", "APPROVED", "REJECTED"}
    )
    String brokerVerificationStatus;

    @Schema(description = "When the broker registration was submitted", example = "2024-01-15T10:30:00")
    LocalDateTime brokerRegisteredAt;

    @Schema(description = "When the admin made the decision", example = "2024-01-16T09:00:00")
    LocalDateTime brokerVerifiedAt;

    @Schema(description = "Admin ID who made the decision", example = "admin-uuid-abc")
    String brokerVerifiedByAdminId;

    @Schema(description = "Rejection reason (only present when REJECTED)", example = "License not found")
    String brokerRejectionReason;

    // ============ DOCUMENT VIEWING URLS (presigned, short-lived) ============

    @Schema(
            description = "Presigned download URL for CCCD front image",
            example = "https://r2.example.com/users/.../broker/....jpg?X-Amz-Signature=..."
    )
    String cccdFrontUrl;

    @Schema(description = "Presigned download URL for CCCD back image")
    String cccdBackUrl;

    @Schema(description = "Presigned download URL for practising certificate front image")
    String certFrontUrl;

    @Schema(description = "Presigned download URL for practising certificate back image")
    String certBackUrl;
}
