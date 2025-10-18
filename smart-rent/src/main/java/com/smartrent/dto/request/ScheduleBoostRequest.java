package com.smartrent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleBoostRequest {

    @NotNull(message = "Listing ID is required")
    Long listingId;

    @NotNull(message = "Scheduled time is required")
    LocalTime scheduledTime;

    @NotNull(message = "Total pushes is required")
    Integer totalPushes;

    Boolean useMembershipQuota;

    String paymentProvider;
}

