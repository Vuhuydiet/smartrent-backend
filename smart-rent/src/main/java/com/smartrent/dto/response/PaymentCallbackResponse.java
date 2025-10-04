package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Generic payment callback response")
public class PaymentCallbackResponse {

    @Schema(description = "Payment ID", example = "12345")
    Long paymentId;

    @Schema(description = "Payment provider")
    PaymentProvider provider;

    @Schema(description = "Transaction reference", example = "TXN123456789")
    String transactionRef;

    @Schema(description = "Provider transaction ID", example = "13863934")
    String providerTransactionId;

    @Schema(description = "Transaction status")
    TransactionStatus status;

    @Schema(description = "Payment amount", example = "100000")
    BigDecimal amount;

    @Schema(description = "Currency code", example = "VND")
    String currency;

    @Schema(description = "Order information", example = "Payment for listing rental")
    String orderInfo;

    @Schema(description = "Payment method used", example = "ATM")
    String paymentMethod;

    @Schema(description = "Bank code", example = "NCB")
    String bankCode;

    @Schema(description = "Bank transaction ID", example = "VNP13863934")
    String bankTransactionId;

    @Schema(description = "Payment completion timestamp")
    LocalDateTime paymentDate;

    @Schema(description = "Provider response code", example = "00")
    String responseCode;

    @Schema(description = "Provider response message", example = "Transaction successful")
    String responseMessage;

    @Schema(description = "Whether the transaction was successful")
    Boolean success;

    @Schema(description = "Signature validation result")
    Boolean signatureValid;

    @Schema(description = "Additional processing message")
    String message;

    @Schema(description = "Provider-specific response data")
    Map<String, Object> providerData;
}
