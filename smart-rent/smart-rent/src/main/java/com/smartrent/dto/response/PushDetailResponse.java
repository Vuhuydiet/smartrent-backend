package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Push pricing details and packages")
public class PushDetailResponse {

    @Schema(description = "Push detail ID", example = "1")
    Long pushDetailId;

    @Schema(description = "Detail code", example = "SINGLE_PUSH")
    String detailCode;

    @Schema(description = "Detail name in Vietnamese", example = "Đẩy tin đơn lẻ")
    String detailName;

    @Schema(description = "Detail name in English", example = "Single Push")
    String detailNameEn;

    // Pricing
    @Schema(description = "Price per single push in VND", example = "40000")
    BigDecimal pricePerPush;

    @Schema(description = "Number of pushes in package", example = "1")
    Integer quantity;

    @Schema(description = "Total price for package in VND", example = "40000")
    BigDecimal totalPrice;

    @Schema(description = "Discount percentage if package", example = "0.00")
    BigDecimal discountPercentage;

    @Schema(description = "Amount saved compared to buying individually", example = "0")
    BigDecimal savings;

    // Description
    @Schema(description = "Push package description")
    String description;

    @Schema(description = "List of feature descriptions")
    List<String> features;

    // Status
    @Schema(description = "Whether package is currently available", example = "true")
    Boolean isActive;

    @Schema(description = "Display order in UI", example = "1")
    Integer displayOrder;
}

