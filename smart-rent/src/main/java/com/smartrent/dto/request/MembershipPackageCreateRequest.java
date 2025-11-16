package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request object for creating a new membership package")
public class MembershipPackageCreateRequest {

    @NotBlank(message = "Package code is required")
    @Size(max = 50, message = "Package code must not exceed 50 characters")
    @Schema(
        description = "Unique code for the membership package",
        example = "PREMIUM_12M",
        maxLength = 50,
        required = true
    )
    String packageCode;

    @NotBlank(message = "Package name is required")
    @Size(max = 100, message = "Package name must not exceed 100 characters")
    @Schema(
        description = "Name of the membership package",
        example = "Premium 12 Months",
        maxLength = 100,
        required = true
    )
    String packageName;

    @NotBlank(message = "Package level is required")
    @Schema(
        description = "Level of the package (BASIC, STANDARD, ADVANCED)",
        example = "ADVANCED",
        required = true
    )
    String packageLevel;

    @NotNull(message = "Duration in months is required")
    @Min(value = 1, message = "Duration must be at least 1 month")
    @Schema(
        description = "Duration of the membership in months",
        example = "12",
        minimum = "1",
        required = true
    )
    Integer durationMonths;

    @NotNull(message = "Original price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Original price must be greater than 0")
    @Schema(
        description = "Original price of the package",
        example = "1200000",
        required = true
    )
    BigDecimal originalPrice;

    @NotNull(message = "Sale price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Sale price must be greater than 0")
    @Schema(
        description = "Sale price of the package",
        example = "999000",
        required = true
    )
    BigDecimal salePrice;

    @Schema(
        description = "Discount percentage",
        example = "16.75"
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

