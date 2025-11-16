package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request object for updating a membership package")
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
        description = "Original price of the package",
        example = "1200000"
    )
    BigDecimal originalPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Sale price must be greater than 0")
    @Schema(
        description = "Sale price of the package",
        example = "999000"
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

