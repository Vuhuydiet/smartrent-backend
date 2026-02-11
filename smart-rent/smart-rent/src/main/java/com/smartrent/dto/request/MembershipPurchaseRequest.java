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
public class MembershipPurchaseRequest {

    @NotNull(message = "Membership package ID is required")
    Long membershipId;

    String paymentProvider; // VNPAY, MOMO, WALLET, etc.
}

