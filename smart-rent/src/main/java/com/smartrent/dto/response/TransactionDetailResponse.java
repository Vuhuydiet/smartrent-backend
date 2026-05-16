package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionDetailResponse {
    String transactionId;
    String transactionCode;
    String idempotencyKey;
    BigDecimal amount;
    String currency;
    String paymentGateway;
    String paymentMethod;
    String gatewayTransactionCode;
    String gatewayResponseCode;
    String status;
    String paymentType;
    LocalDateTime createdAt;
    LocalDateTime completedAt;
    LocalDateTime expiredAt;
    TransactionInvoiceResponse invoice;
    TransactionRoomResponse room;
    TransactionPartyResponse customer;
    TransactionPartyResponse landlord;
    String failureReason;
    String orderInfo;
    String providerPayload;
    List<TransactionTimelineResponse> timeline;
}
