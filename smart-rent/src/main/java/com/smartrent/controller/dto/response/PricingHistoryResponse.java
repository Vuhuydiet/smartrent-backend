package com.smartrent.controller.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingHistoryResponse {
    Long id;
    Long listingId;
    BigDecimal oldPrice;
    BigDecimal newPrice;
    String oldPriceUnit;
    String newPriceUnit;
    String changeType;
    BigDecimal changePercentage;
    BigDecimal changeAmount;
    boolean isCurrent;
    String changedBy;
    String changeReason;
    LocalDateTime changedAt;
}
