package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to create or update a duration plan")
public class DurationPlanRequest {

    @NotNull(message = "Duration days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    @Schema(description = "Duration in days", example = "45", required = true)
    Integer durationDays;

    @Schema(description = "Whether the plan is active", example = "true")
    @Builder.Default
    Boolean isActive = true;
}
