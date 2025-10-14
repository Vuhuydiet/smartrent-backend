package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for filtering data by date range")
public class DateRangeRequest {

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Start date and time", example = "2025-01-01T00:00:00", required = true)
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "End date and time", example = "2025-12-31T23:59:59", required = true)
    private LocalDateTime endDate;
}