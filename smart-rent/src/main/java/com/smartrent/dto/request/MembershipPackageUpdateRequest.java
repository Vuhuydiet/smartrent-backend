package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request object for updating a membership package. Sale price is automatically calculated by the server from originalPrice and discountPercentage; admins do not provide salePrice directly.")
public class MembershipPackageUpdateRequest {

    @Size(max = 100, message = "Package name must not exceed 100 characters")
    @Schema(
        description = "Name of the membership package",
        example = "Premium 12 Months",
        maxLength = 100
    )
    String packageName;

    @Schema(
        description = "Level of the package (BASIC, STANDARD, ADVANCED)",
        example = "ADVANCED"
    )
    String packageLevel;

    @Min(value = 1, message = "Duration must be at least 1 month")
    @Schema(
        description = "Duration of the membership in months",
        example = "12",
        minimum = "1"
    )
    Integer durationMonths;

    @DecimalMin(value = "0.0", inclusive = false, message = "Original price must be greater than 0")
    @Schema(
        description = "Original price of the package (before discount)",
        example = "1200000"
    )
    BigDecimal originalPrice;

    @DecimalMin(value = "0.00", inclusive = true, message = "Discount percentage must be at least 0")
    @DecimalMax(value = "100.00", inclusive = true, message = "Discount percentage must not exceed 100")
    @Schema(
        description = "Discount percentage (0-100). Server will automatically recalculate salePrice = originalPrice * (1 - discountPercentage / 100), rounded to 0 decimal places.",
        example = "20.00",
        minimum = "0",
        maximum = "100"
    )
    BigDecimal discountPercentage;

    @Schema(
        description = "Whether the package is active",
        example = "true"
    )
    Boolean isActive;

    @Schema(
        description = "Description of the package",
        example = "Premium membership with all features for 12 months"
    )
    String description;
}
