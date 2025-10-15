package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartrent.enums.PaymentProvider;
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
@Schema(description = "Generic payment response")
public class PaymentResponse {

    @Schema(description = "Payment ID", example = "12345")
    Long paymentId;

    @Schema(description = "Payment provider")
    PaymentProvider provider;

    @Schema(description = "Transaction reference", example = "TXN123456789")
    String transactionRef;

    @Schema(description = "Provider transaction ID", example = "13863934")
    String providerTransactionId;

    @Schema(description = "Payment URL or redirect URL")
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

    @Schema(description = "Provider-specific response data")
    Map<String, Object> providerData;
}
