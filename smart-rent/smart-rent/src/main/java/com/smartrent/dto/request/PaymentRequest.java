package com.smartrent.dto.request;

import com.smartrent.enums.PaymentProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Generic payment request")
public class PaymentRequest {

    @NotNull(message = "Payment provider is required")
    @Schema(description = "Payment provider", example = "VNPAY")
    PaymentProvider provider;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Amount must be at least 1,000 VND")
    @DecimalMax(value = "1000000000", message = "Amount must not exceed 1,000,000,000 VND")
    @Schema(description = "Payment amount", example = "100000", minimum = "1000", maximum = "1000000000")
    BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(max = 3, message = "Currency must be 3 characters")
    @Schema(description = "Currency code", example = "VND")
    String currency;

    @NotBlank(message = "Order info is required")
    @Size(max = 255, message = "Order info must not exceed 255 characters")
    @Schema(description = "Order information", example = "Payment for listing rental deposit")
    String orderInfo;

    @Schema(description = "Existing transaction ID to use (optional - if provided, will reuse existing transaction instead of creating new one)")
    String transactionId;

    @Schema(description = "Listing ID for the payment", example = "12345")
    Long listingId;

    @Size(max = 1000, message = "Return URL must not exceed 1000 characters")
    @Schema(description = "Custom return URL after payment completion")
    String returnUrl;

    @Size(max = 1000, message = "Cancel URL must not exceed 1000 characters")
    @Schema(description = "Custom cancel URL for payment cancellation")
    String cancelUrl;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Schema(description = "Additional notes for the payment")
    String notes;

    @Schema(description = "Additional metadata as key-value pairs")
    Map<String, String> metadata;

    @Schema(description = "Provider-specific parameters")
    Map<String, Object> providerParams;
}
