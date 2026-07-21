package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Deterministic price statistics over the comparable listings that matched the
 * requested geo/type criteria. All figures are computed server-side (SQL filter
 * + Java percentiles) so the numbers are stable and reproducible — the caller
 * consumes them directly instead of asking an LLM to total up raw listings.
 */
@Getter
@Setter
@Builder
@Schema(description = "Aggregate price statistics over comparable listings")
public class PriceComparablesResponse {

    @Schema(description = "Number of comparable listings the statistics are based on", example = "42")
    Integer sampleSize;

    @Schema(description = "Cheapest comparable price (VND)", example = "3200000")
    Long min;

    @Schema(description = "25th percentile price (VND) — robust lower bound", example = "4100000")
    Long p25;

    @Schema(description = "Median price (VND)", example = "4800000")
    Long median;

    @Schema(description = "75th percentile price (VND) — robust upper bound", example = "5600000")
    Long p75;

    @Schema(description = "Most expensive comparable price (VND)", example = "9000000")
    Long max;

    @Schema(description = "Mean price (VND)", example = "4900000")
    Long avg;

    @Schema(description = "Median price per m² across the comparables (VND/m²)", example = "125000")
    Long pricePerSqmMedian;

    @Schema(description = "Currency code", example = "VND")
    @Builder.Default
    String currency = "VND";
}
