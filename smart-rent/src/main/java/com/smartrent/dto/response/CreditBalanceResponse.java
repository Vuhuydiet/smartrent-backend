package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Response containing user credit balance information")
public class CreditBalanceResponse {

    @Schema(description = "User ID", example = "12345")
    Long userId;

    @Schema(description = "Current credit balance", example = "250.75")
    BigDecimal balance;

    @Schema(description = "Currency code", example = "VND")
    String currency;

    @Schema(description = "Last updated timestamp")
    LocalDateTime lastUpdated;

    @Schema(description = "Total credits added", example = "1000.00")
    BigDecimal totalCreditsAdded;

    @Schema(description = "Total credits spent", example = "749.25")
    BigDecimal totalCreditsSpent;
}