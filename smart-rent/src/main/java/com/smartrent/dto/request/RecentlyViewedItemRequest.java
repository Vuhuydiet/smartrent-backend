package com.smartrent.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class RecentlyViewedItemRequest {
    @NotNull(message = "Listing ID is required")
    Long listingId;

    @NotNull(message = "Viewed At timestamp is required")
    Long viewedAt; // epoch in milliseconds
}
