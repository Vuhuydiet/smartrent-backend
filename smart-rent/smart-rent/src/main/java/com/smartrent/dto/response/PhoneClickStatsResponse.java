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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Statistics about phone clicks for a listing")
public class PhoneClickStatsResponse {

    @Schema(description = "Listing ID", example = "123")
    Long listingId;

    @Schema(description = "Total number of phone clicks", example = "25")
    Long totalClicks;

    @Schema(description = "Number of unique users who clicked", example = "18")
    Long uniqueUsers;
}

