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
@Schema(description = "Statistics about phone clicks across all listings owned by a user")
public class OwnerPhoneClickStatsResponse {

    @Schema(description = "Total number of phone clicks across all of the owner's listings", example = "137")
    Long totalClicks;

    @Schema(description = "Number of unique users who clicked across all of the owner's listings", example = "42")
    Long uniqueUsers;
}
