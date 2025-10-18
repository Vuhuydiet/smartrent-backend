package com.smartrent.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
}
