package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response for credit transaction operations")
public class CreditTransactionResponse {

    @Schema(description = "Transaction ID", example = "12345")
    Long transactionId;

    @Schema(description = "User ID", example = "67890")
    Long userId;

    @Schema(description = "Transaction type", example = "CREDIT_ADD")
    String transactionType;

    @Schema(description = "Amount involved in transaction", example = "100.00")
    BigDecimal amount;

    @Schema(description = "Currency code", example = "VND")
    String currency;

    @Schema(description = "Balance after transaction", example = "350.75")
    BigDecimal balanceAfter;

    @Schema(description = "Reason for transaction", example = "Payment refund")
    String reason;

    @Schema(description = "Reference transaction ID", example = "TXN123456789")
    String referenceTransactionId;

    @Schema(description = "Transaction timestamp")
    LocalDateTime transactionDate;

    @Schema(description = "Success status", example = "true")
    Boolean success;

    @Schema(description = "Response message", example = "Credit added successfully")
    String message;
}