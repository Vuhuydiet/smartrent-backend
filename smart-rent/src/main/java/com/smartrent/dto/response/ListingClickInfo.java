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
@Schema(description = "Information about a listing that was clicked by a user")
public class ListingClickInfo {

    @Schema(description = "Listing ID", example = "123")
    Long listingId;

    @Schema(description = "Listing title", example = "Beautiful 2BR Apartment in District 1")
    String listingTitle;

    @Schema(description = "When the phone number was clicked", example = "2024-01-15T10:30:00")
    LocalDateTime clickedAt;

    @Schema(description = "Total number of clicks by this user on this listing", example = "3")
    Integer clickCount;
}

