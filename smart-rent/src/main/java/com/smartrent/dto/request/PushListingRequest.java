package com.smartrent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushListingRequest {

    @NotNull(message = "Listing ID is required")
    Long listingId;

    Boolean useMembershipQuota; // true = use quota, false = direct purchase

    String paymentProvider; // Only needed if useMembershipQuota = false

    String returnUrl; // URL to return after payment (for VNPay callback)
}
