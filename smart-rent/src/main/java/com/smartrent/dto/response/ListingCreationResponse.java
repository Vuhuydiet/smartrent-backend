package com.smartrent.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

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
    String message;
}

