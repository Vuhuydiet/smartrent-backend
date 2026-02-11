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
@Schema(description = "Response containing phone click tracking information")
public class PhoneClickResponse {

    @Schema(description = "Phone click ID", example = "1")
    Long id;

    @Schema(description = "Listing ID", example = "123")
    Long listingId;

    @Schema(description = "Listing title", example = "Beautiful 2BR Apartment in District 1")
    String listingTitle;

    @Schema(description = "User ID who clicked", example = "user-123e4567-e89b-12d3-a456-426614174000")
    String userId;

    @Schema(description = "User's first name", example = "John")
    String userFirstName;

    @Schema(description = "User's last name", example = "Doe")
    String userLastName;

    @Schema(description = "User's email", example = "john.doe@example.com")
    String userEmail;

    @Schema(description = "User's contact phone number", example = "0912345678")
    String userContactPhone;

    @Schema(description = "Whether user's contact phone is verified", example = "true")
    Boolean userContactPhoneVerified;

    @Schema(description = "User's avatar URL", example = "https://lh3.googleusercontent.com/a/example")
    String userAvatarUrl;

    @Schema(description = "When the phone number was clicked", example = "2024-01-15T10:30:00")
    LocalDateTime clickedAt;

    @Schema(description = "IP address of the click", example = "192.168.1.1")
    String ipAddress;
}

