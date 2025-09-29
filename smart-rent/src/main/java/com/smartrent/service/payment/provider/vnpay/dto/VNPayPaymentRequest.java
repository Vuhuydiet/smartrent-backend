package com.smartrent.service.payment.provider.vnpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to create VNPay payment")
public class VNPayPaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Amount must be at least 1,000 VND")
    @DecimalMax(value = "1000000000", message = "Amount must not exceed 1,000,000,000 VND")
    @Schema(description = "Payment amount in VND", example = "100000", minimum = "1000", maximum = "1000000000")
    BigDecimal amount;

    @NotBlank(message = "Order info is required")
    @Size(max = 255, message = "Order info must not exceed 255 characters")
    @Schema(description = "Order information", example = "Payment for listing rental deposit")
    String orderInfo;

    @Schema(description = "Listing ID for the payment", example = "12345")
    Long listingId;

    @Size(max = 20, message = "Bank code must not exceed 20 characters")
    @Schema(description = "Bank code for payment (optional)", example = "NCB")
    String bankCode;

    @Size(max = 10, message = "Language must not exceed 10 characters")
    @Schema(description = "Language preference", example = "vn", defaultValue = "vn")
    String language;

    @Size(max = 1000, message = "Return URL must not exceed 1000 characters")
    @Schema(description = "Custom return URL after payment completion")
    String returnUrl;

    @Size(max = 1000, message = "Cancel URL must not exceed 1000 characters")
    @Schema(description = "Custom cancel URL for payment cancellation")
    String cancelUrl;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Schema(description = "Additional notes for the payment")
    String notes;

    @Schema(description = "Additional metadata as JSON string")
    String metadata;
}
