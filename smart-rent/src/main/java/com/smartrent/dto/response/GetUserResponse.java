package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response object containing user profile information")
public class GetUserResponse implements Serializable {

    @Schema(description = "Unique identifier for the user", example = "user-123e4567-e89b-12d3-a456-426614174000")
    String userId;

    @Schema(description = "International phone code (country code)", example = "+1")
    String phoneCode;

    @Schema(description = "Phone number without country code", example = "1234567890")
    String phoneNumber;

    @Schema(description = "User's email address", example = "john.doe@example.com")
    String email;

    @Schema(description = "User's first name", example = "John")
    String firstName;

    @Schema(description = "User's last name", example = "Doe")
    String lastName;

    @Schema(description = "Account creation time", example = "2024-05-01T10:15:30")
    LocalDateTime createdAt;

    @Schema(description = "User's identification document number", example = "ID123456789")
    String idDocument;

    @Schema(description = "User's tax identification number", example = "TAX987654321")
    String taxNumber;

    @Schema(description = "Vietnam contact phone number for Zalo or other messaging", example = "0912345678")
    String contactPhoneNumber;

    @Schema(description = "Whether the contact phone number has been verified", example = "true")
    Boolean contactPhoneVerified;

    @Schema(description = "URL of the user's profile picture", example = "https://lh3.googleusercontent.com/a/example")
    String avatarUrl;

    @Schema(description = "Media record ID backing the avatar (null for legacy avatars or external OAuth avatars)", example = "12345")
    Long avatarMediaId;

    // ============ BROKER BADGE FIELDS ============

    @Schema(description = "Whether this user is a verified broker. Null means not available (backward compat).", example = "false")
    Boolean isBroker;

    @Schema(description = "Current broker verification status: NONE | PENDING | APPROVED | REJECTED", example = "NONE", allowableValues = {
            "NONE", "PENDING", "APPROVED", "REJECTED" })
    String brokerVerificationStatus;
}
