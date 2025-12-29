package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Recently viewed listing response")
public class RecentlyViewedItemResponse {

    @Schema(
            description = "ID of the listing that was viewed",
            example = "123"
    )
    Long listingId;

    @Schema(
            description = "Timestamp when the listing was viewed (epoch milliseconds)",
            example = "1703592000000"
    )
    Long viewedAt;
}