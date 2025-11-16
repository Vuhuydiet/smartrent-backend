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
@Schema(description = "Price calculation result for a listing based on tier and duration")
public class PriceCalculationResponse {

    @Schema(description = "VIP tier (NORMAL, SILVER, GOLD, DIAMOND)", example = "SILVER")
    String vipType;

    @Schema(description = "Duration in days", example = "30")
    Integer durationDays;

    @Schema(description = "Base price per day", example = "50000")
    BigDecimal basePricePerDay;

    @Schema(description = "Total price before discount", example = "1500000")
    BigDecimal totalBeforeDiscount;

    @Schema(description = "Discount percentage (0-1)", example = "0.185")
    BigDecimal discountPercentage;

    @Schema(description = "Discount amount", example = "277500")
    BigDecimal discountAmount;

    @Schema(description = "Final price after discount", example = "1222500")
    BigDecimal finalPrice;

    @Schema(description = "Currency", example = "VND")
    String currency;

    @Schema(description = "Savings description", example = "Save 277,500 VND (18.5%)")
    String savingsDescription;
}
