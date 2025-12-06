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

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing user details with their clicked listings")
public class UserPhoneClickDetailResponse {

    @Schema(description = "User ID", example = "user-123e4567-e89b-12d3-a456-426614174000")
    String userId;

    @Schema(description = "User's first name", example = "John")
    String firstName;

    @Schema(description = "User's last name", example = "Doe")
    String lastName;

    @Schema(description = "User's email", example = "john.doe@example.com")
    String email;

    @Schema(description = "User's contact phone number", example = "0912345678")
    String contactPhone;

    @Schema(description = "Whether user's contact phone is verified", example = "true")
    Boolean contactPhoneVerified;

    @Schema(description = "User's avatar URL", example = "https://example.com/avatar.jpg")
    String avatarUrl;

    @Schema(description = "Total number of listings clicked by this user", example = "5")
    Integer totalListingsClicked;

    @Schema(description = "List of listings that this user has clicked on phone numbers")
    List<ListingClickInfo> clickedListings;
}

