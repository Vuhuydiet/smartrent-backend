package com.smartrent.service.payment.provider.vnpay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@Schema(description = "VNPay payment creation response")
public class VNPayPaymentResponse {

    @Schema(description = "Payment ID", example = "12345")
    Long paymentId;

    @Schema(description = "Transaction reference", example = "TXN123456789")
    String transactionRef;

    @Schema(description = "VNPay payment URL")
    String paymentUrl;

    @Schema(description = "Payment amount", example = "100000")
    BigDecimal amount;

    @Schema(description = "Currency code", example = "VND")
    String currency;

    @Schema(description = "Order information", example = "Payment for listing rental")
    String orderInfo;

    @Schema(description = "Payment creation timestamp")
    LocalDateTime createdAt;

    @Schema(description = "Payment expiration timestamp")
    LocalDateTime expiresAt;

    @Schema(description = "QR code data for mobile payment")
    String qrCodeData;

    @Schema(description = "Deep link for mobile app payment")
    String deepLink;
}
