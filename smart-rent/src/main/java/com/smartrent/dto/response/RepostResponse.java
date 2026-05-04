package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    String message;

    // Only populated when payment is required
    String paymentUrl;
    String transactionId;
}
