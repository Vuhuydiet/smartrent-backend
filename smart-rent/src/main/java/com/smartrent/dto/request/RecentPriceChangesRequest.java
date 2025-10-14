package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for fetching listings with recent price changes")
public class RecentPriceChangesRequest {

    @Builder.Default
    @Min(value = 1, message = "Days back must be at least 1")
    @Schema(description = "Number of days to look back for price changes", example = "7", defaultValue = "7")
    private Integer daysBack = 7;
}