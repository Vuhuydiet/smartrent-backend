package com.smartrent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request to repost (re-publish) an expired listing.
 * Mirrors {@link PushListingRequest} — user picks between consuming the
 * matching membership quota for their vipType or paying the per-day fee
 * via VNPay.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepostListingRequest {

    @NotNull(message = "Listing ID is required")
    Long listingId;

    /** true = consume matching membership quota; false = direct payment. */
    Boolean useMembershipQuota;

    /** Only used when useMembershipQuota = false. Defaults to VNPAY. */
    String paymentProvider;

    /**
     * New active duration in days (10 / 15 / 30). When null, falls back to
     * the listing's existing durationDays (or 30 if that is also null).
     */
    Integer durationDays;
}
