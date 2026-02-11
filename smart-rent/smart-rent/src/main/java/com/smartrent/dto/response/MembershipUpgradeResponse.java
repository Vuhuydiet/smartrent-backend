package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for membership upgrade initiation
 * Contains payment URL and upgrade transaction details
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MembershipUpgradeResponse {

    // Transaction info
    String transactionRef;
    String paymentUrl;
    String paymentProvider;

    // Upgrade details
    Long previousMembershipId;
    Long newMembershipPackageId;
    String newPackageName;
    String newPackageLevel;

    // Pricing
    BigDecimal originalPrice;
    BigDecimal discountAmount;
    BigDecimal finalAmount;

    // Status
    String status;
    String message;

    // Timestamps
    LocalDateTime createdAt;
    LocalDateTime expiresAt;
}

