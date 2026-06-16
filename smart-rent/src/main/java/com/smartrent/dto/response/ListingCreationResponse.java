package com.smartrent.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingCreationResponse {
    Long listingId;
    String status; // e.g., "CREATED"

    // Payment-related fields (for listings requiring payment)
    Boolean paymentRequired;
    String transactionId;
    Long amount;
    String paymentUrl;
    // Payment provider (e.g. "SEPAY") + provider-specific checkout data. SePay
    // returns signed form fields that the FE must POST to the hosted checkout;
    // without providerData the FE cannot start the SePay checkout.
    String provider;
    Map<String, Object> providerData;
    String message;
}

