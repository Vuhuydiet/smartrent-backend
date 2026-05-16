package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionStatisticsResponse {
    BigDecimal totalRevenue;
    long totalTransactions;
    long successfulPayments;
    long failedPayments;
    long pendingPayments;
    long cancelledPayments;
    long refundedPayments;
    double successRate;
    BigDecimal averageSuccessfulAmount;
}
