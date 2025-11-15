package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Duration plan for listings with pricing information")
public class ListingDurationPlanResponse {

    @Schema(description = "Plan ID", example = "1")
    Long planId;

    @Schema(description = "Duration in days", example = "30")
    Integer durationDays;

    @Schema(description = "Whether the plan is active", example = "true")
    Boolean isActive;

    @Schema(description = "Discount percentage (0-1)", example = "0.185")
    BigDecimal discountPercentage;

    @Schema(description = "Discount description", example = "18.5% off")
    String discountDescription;

    @Schema(description = "Price for NORMAL tier", example = "66000")
    BigDecimal normalPrice;

    @Schema(description = "Price for SILVER tier", example = "1222500")
    BigDecimal silverPrice;

    @Schema(description = "Price for GOLD tier", example = "2689500")
    BigDecimal goldPrice;

    @Schema(description = "Price for DIAMOND tier", example = "6846000")
    BigDecimal diamondPrice;
}
