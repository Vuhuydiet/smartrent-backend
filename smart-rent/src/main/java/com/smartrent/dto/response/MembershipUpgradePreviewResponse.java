package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for membership upgrade preview
 * Shows the user what they will pay and what discount they receive
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MembershipUpgradePreviewResponse {

    // Current membership info
    Long currentMembershipId;
    String currentPackageName;
    String currentPackageLevel;
    Long daysRemaining;

    // Target membership info
    Long targetMembershipId;
    String targetPackageName;
    String targetPackageLevel;
    Integer targetDurationDays;
    BigDecimal targetPackagePrice;

    // Discount calculation details
    BigDecimal discountAmount;
    BigDecimal discountPercentage;

    // Final pricing
    BigDecimal finalPrice;

    // Benefits info
    List<ForfeitedBenefitInfo> forfeitedBenefits;
    List<MembershipPackageBenefitResponse> newBenefits;

    // Upgrade eligibility
    Boolean eligible;
    String ineligibilityReason;

    /**
     * Inner class to show forfeited benefit details
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ForfeitedBenefitInfo {
        String benefitType;
        String benefitName;
        Integer totalQuantity;
        Integer usedQuantity;
        Integer remainingQuantity;
        BigDecimal estimatedValue;
    }
}

