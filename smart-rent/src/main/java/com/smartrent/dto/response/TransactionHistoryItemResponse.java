package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionHistoryItemResponse {
    String transactionId;
    String transactionCode;
    BigDecimal amount;
    String currency;
    String paymentGateway;
    String paymentMethod;
    String gatewayTransactionCode;
    String status;
    String paymentType;
    LocalDateTime createdAt;
    LocalDateTime completedAt;
    TransactionInvoiceResponse invoice;
    TransactionRoomResponse room;
    TransactionPartyResponse customer;
    TransactionPartyResponse landlord;
    String failureReason;
}
