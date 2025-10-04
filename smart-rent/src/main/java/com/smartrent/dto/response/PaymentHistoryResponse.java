package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payment history response")
public class PaymentHistoryResponse {

    @Schema(description = "Payment ID", example = "12345")
    Long paymentId;

    @Schema(description = "Transaction reference", example = "TXN123456789")
    String transactionRef;

    @Schema(description = "Provider transaction ID", example = "13863934")
    String providerTransactionId;

    @Schema(description = "Payment amount", example = "100000")
    BigDecimal amount;

    @Schema(description = "Currency code", example = "VND")
    String currency;

    @Schema(description = "Transaction type")
    TransactionType transactionType;

    @Schema(description = "Transaction status")
    TransactionStatus status;

    @Schema(description = "Order information", example = "Payment for listing rental")
    String orderInfo;

    @Schema(description = "Payment method used", example = "ATM")
    String paymentMethod;

    @Schema(description = "Payment completion timestamp")
    LocalDateTime paymentDate;

    @Schema(description = "Listing ID associated with payment", example = "67890")
    Long listingId;

    @Schema(description = "User ID who made the payment", example = "11111")
    Long userId;

    @Schema(description = "Payment creation timestamp")
    LocalDateTime createdAt;

    @Schema(description = "Payment last update timestamp")
    LocalDateTime updatedAt;

    @Schema(description = "Additional notes")
    String notes;
}
