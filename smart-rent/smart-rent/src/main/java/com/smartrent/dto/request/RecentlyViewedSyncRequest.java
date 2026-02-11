package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to sync recently viewed listings from client")
public class RecentlyViewedSyncRequest {

    @NotNull(message = "Listings list is required (can be empty)")
    @Valid
    @Schema(
            description = "List of recently viewed listings from client localStorage. Can be empty for first-time sync.",
            example = "[{\"listingId\": 123, \"viewedAt\": 1703592000000}, {\"listingId\": 456, \"viewedAt\": 1703595600000}]",
            required = true
    )
    List<RecentlyViewedItemDto> listings;
}