package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushResponse {

    Long pushId;
    Long listingId;
    String userId;
    String pushSource;
    LocalDateTime pushedAt;
    String message;

    // Payment-related fields (only populated when payment is required)
    String paymentUrl;
    String transactionId;
    // Payment provider (e.g. "SEPAY") + provider-specific checkout data. SePay
    // returns signed form fields the FE must POST to the hosted checkout;
    // without providerData the FE cannot start the SePay checkout.
    String provider;
    Map<String, Object> providerData;
}
