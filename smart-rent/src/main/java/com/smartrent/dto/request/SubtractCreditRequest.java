package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to subtract credit from user wallet")
public class SubtractCreditRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID to subtract credit from", example = "12345")
    Long userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "Amount to subtract from user credit", example = "50.00")
    BigDecimal amount;

    @Schema(description = "Currency code", example = "VND")
    String currency;

    @Schema(description = "Reason for subtracting credit", example = "Service payment")
    String reason;

    @Schema(description = "Reference transaction ID", example = "TXN123456789")
    String referenceTransactionId;
}