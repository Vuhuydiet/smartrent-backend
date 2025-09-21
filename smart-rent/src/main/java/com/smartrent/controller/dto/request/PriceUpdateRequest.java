package com.smartrent.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceUpdateRequest {
    @NotNull(message = "New price is required")
    @Positive(message = "Price must be positive")
    BigDecimal newPrice;

    @NotNull(message = "Price unit is required")
    String priceUnit; // MONTH, DAY, YEAR

    String changeReason;
}
