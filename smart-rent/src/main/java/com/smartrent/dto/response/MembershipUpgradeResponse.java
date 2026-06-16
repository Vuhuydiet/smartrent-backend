package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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
    // Provider-specific checkout data. SePay returns signed form fields the FE
    // must POST to the hosted checkout; without providerData the FE cannot
    // start the SePay checkout.
    Map<String, Object> providerData;

    /**
     * Provider-specific data needed to start the checkout, mirroring
     * {@link PaymentResponse#getProviderData()}. For SePay this carries the hosted-checkout
     * {@code method}, {@code checkoutUrl} and the signed {@code fields} the frontend must
     * render as a hidden form and auto-submit via POST. Without it the frontend would only
     * have {@code paymentUrl} and would navigate to it with a GET, which SePay answers with 404.
     */
    Map<String, Object> providerData;

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

