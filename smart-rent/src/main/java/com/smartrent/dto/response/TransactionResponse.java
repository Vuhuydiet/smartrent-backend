package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionResponse {

    String transactionId;
    String userId;
    String transactionType;
    BigDecimal amount;
    BigDecimal balanceBefore;
    BigDecimal balanceAfter;
    String referenceType;
    String referenceId;
    String additionalInfo;
    String status;
    String paymentProvider;
    String providerTransactionId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

