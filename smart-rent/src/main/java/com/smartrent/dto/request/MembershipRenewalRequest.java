package com.smartrent.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MembershipRenewalRequest {

    /**
     * Payment provider to use. Defaults to SEPAY if not specified.
     */
    String paymentProvider;
}
