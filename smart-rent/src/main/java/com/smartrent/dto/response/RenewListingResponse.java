package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response from a successful renew (gia hạn) operation. Renewal is always
 * quota-only and cumulative — the new {@code expiryDate} is the previous
 * expiry plus {@link #daysAdded} (30) days.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RenewListingResponse {

    Long listingId;
    String userId;

    LocalDateTime renewedAt;
    /** Previous expiry — what the new expiry was extended from. */
    LocalDateTime previousExpiryDate;
    /** New expiry — previousExpiryDate plus daysAdded. */
    LocalDateTime expiryDate;
    Integer daysAdded;
    String vipType;
    String message;
}
