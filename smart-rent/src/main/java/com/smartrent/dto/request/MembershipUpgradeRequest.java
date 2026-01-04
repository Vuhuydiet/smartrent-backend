package com.smartrent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for membership upgrade
 * Used when a user wants to upgrade from their current membership to a higher tier
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MembershipUpgradeRequest {

    /**
     * The ID of the target membership package to upgrade to
     * Must be a higher tier than the current membership
     */
    @NotNull(message = "Target membership package ID is required")
    Long targetMembershipId;

    /**
     * Payment provider to use for the upgrade payment
     * Defaults to VNPAY if not specified
     */
    String paymentProvider;
}

