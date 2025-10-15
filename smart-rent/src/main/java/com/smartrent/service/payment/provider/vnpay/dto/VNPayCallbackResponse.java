package com.smartrent.service.payment.provider.vnpay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartrent.service.payment.provider.vnpay.VNPayTransactionStatus;
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
@Schema(description = "VNPay callback processing response")
public class VNPayCallbackResponse {

    @Schema(description = "Payment ID", example = "12345")
    Long paymentId;

    @Schema(description = "Transaction reference", example = "TXN123456789")
    String transactionRef;

    @Schema(description = "VNPay transaction ID", example = "13863934")
    String vnpayTransactionId;

    @Schema(description = "Transaction status")
    VNPayTransactionStatus status;

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

    @Schema(description = "VNPay response code", example = "00")
    String responseCode;

    @Schema(description = "VNPay response message", example = "Giao dịch thành công")
    String responseMessage;

    @Schema(description = "Whether the transaction was successful")
    Boolean success;

    @Schema(description = "Signature validation result")
    Boolean signatureValid;

    @Schema(description = "Additional processing message")
    String message;
}
