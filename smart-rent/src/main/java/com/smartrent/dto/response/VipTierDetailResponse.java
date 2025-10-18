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
@Schema(description = "VIP tier details with pricing and features")
public class VipTierDetailResponse {

    @Schema(description = "Tier ID", example = "1")
    Long tierId;

    @Schema(description = "Tier code", example = "SILVER", allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"})
    String tierCode;

    @Schema(description = "Tier name in Vietnamese", example = "VIP Bạc")
    String tierName;

    @Schema(description = "Tier name in English", example = "VIP Silver")
    String tierNameEn;

    @Schema(description = "Tier level (priority)", example = "2")
    Integer tierLevel;

    // Pricing
    @Schema(description = "Price per day in VND", example = "50000")
    BigDecimal pricePerDay;

    @Schema(description = "Total price for 10 days", example = "500000")
    BigDecimal price10Days;

    @Schema(description = "Total price for 15 days (11% discount)", example = "667500")
    BigDecimal price15Days;

    @Schema(description = "Total price for 30 days (18.5% discount)", example = "1222500")
    BigDecimal price30Days;

    // Features
    @Schema(description = "Maximum number of images allowed", example = "10")
    Integer maxImages;

    @Schema(description = "Maximum number of videos allowed", example = "2")
    Integer maxVideos;

    @Schema(description = "Whether tier has a badge", example = "true")
    Boolean hasBadge;

    @Schema(description = "Badge display name", example = "VIP BẠC")
    String badgeName;

    @Schema(description = "Badge color", example = "blue")
    String badgeColor;

    @Schema(description = "Auto-approve listings without review", example = "true")
    Boolean autoApprove;

    @Schema(description = "No advertisement banners", example = "true")
    Boolean noAds;

    @Schema(description = "Priority in search results", example = "true")
    Boolean priorityDisplay;

    @Schema(description = "Creates shadow normal listing (Diamond only)", example = "false")
    Boolean hasShadowListing;

    // Description
    @Schema(description = "Tier description")
    String description;

    @Schema(description = "List of feature descriptions")
    List<String> features;

    // Status
    @Schema(description = "Whether tier is currently available", example = "true")
    Boolean isActive;

    @Schema(description = "Display order in UI", example = "2")
    Integer displayOrder;
}

