package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Lightweight DTO for JWT token claims - only includes non-temporal fields
 * to avoid serialization issues with Gson (used by nimbus-jose-jwt).
 * Does not include LocalDateTime fields which Gson cannot serialize without a
 * custom TypeAdapter.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "User information for JWT token claims")
public class JwtUserClaimsDto {

	@Schema(description = "Unique identifier for the user", example = "user-123e4567-e89b-12d3-a456-426614174000")
	String userId;

	@Schema(description = "International phone code (country code)", example = "+1")
	String phoneCode;

	@Schema(description = "Phone number without country code", example = "9876543210")
	String phoneNumber;

	@Schema(description = "User's email address", example = "user@smartrent.com")
	String email;

	@Schema(description = "User's first name", example = "John")
	String firstName;

	@Schema(description = "User's last name", example = "Doe")
	String lastName;

	@Schema(description = "Whether the user is verified", example = "true")
	Boolean isVerified;

	@Schema(description = "Whether the user is a broker", example = "false")
	Boolean isBroker;

	@Schema(description = "User's avatar URL", example = "https://example.com/avatar.jpg")
	String avatarUrl;
}
