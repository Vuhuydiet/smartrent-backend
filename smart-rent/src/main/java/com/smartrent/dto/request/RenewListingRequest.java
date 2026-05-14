package com.smartrent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request to renew (gia hạn) an active listing — extends the existing
 * expiryDate by a fixed 30 days cumulatively. Always consumes one quota
 * credit of the listing's current VIP tier ({@code POST_SILVER}/
 * {@code POST_GOLD}/{@code POST_DIAMOND}). NORMAL listings cannot be renewed
 * via this endpoint.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RenewListingRequest {

    @NotNull(message = "Listing ID is required")
    Long listingId;
}
