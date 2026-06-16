package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response from repost (re-publish) operations.
 *
 * <p>When the user pays directly the response carries {@code paymentUrl} +
 * {@code transactionId} and the listing is NOT yet reactivated — that
 * happens in the payment completion callback. When the user uses
 * membership quota the listing is already live and {@code repostedAt}
 * + {@code expiryDate} are populated.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepostResponse {

    Long listingId;
    String userId;

    /** "MEMBERSHIP_QUOTA", "DIRECT_PAYMENT", "PAYMENT_REQUIRED". */
    String repostSource;

    LocalDateTime repostedAt;
    LocalDateTime expiryDate;
    Integer durationDays;
    /** Final VIP tier applied to the listing after repost. */
    String vipType;
    String message;

    // Only populated when payment is required
    String paymentUrl;
    String transactionId;
    // Payment provider (e.g. "SEPAY") + provider-specific checkout data. SePay
    // returns signed form fields the FE must POST to the hosted checkout;
    // without providerData the FE cannot start the SePay checkout.
    String provider;
    Map<String, Object> providerData;
}
