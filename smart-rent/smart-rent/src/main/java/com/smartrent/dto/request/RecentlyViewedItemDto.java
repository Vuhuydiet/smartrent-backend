package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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
@Schema(description = "A single recently viewed listing with timestamp")
public class RecentlyViewedItemDto {

    @NotNull(message = "Listing ID is required")
    @Schema(
            description = "ID of the listing that was viewed",
            example = "123",
            required = true
    )
    Long listingId;

    @NotNull(message = "Viewed timestamp is required")
    @Min(value = 0, message = "Timestamp must not be negative")
    @Schema(
            description = "Timestamp when the listing was viewed (epoch milliseconds)",
            example = "1703592000000",
            required = true
    )
    Long viewedAt;
}