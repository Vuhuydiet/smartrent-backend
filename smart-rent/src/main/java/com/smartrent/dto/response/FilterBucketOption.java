package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "One selectable bucket of a dynamic filter (price/area/bedrooms), "
        + "with the live count of listings it would match under the current filter context")
public class FilterBucketOption {

    @Schema(description = "Stable identifier matching a frontend i18n label", example = "3to7M")
    String key;

    @Schema(description = "Lower bound (inclusive), null = no lower bound", example = "3000000")
    Double min;

    @Schema(description = "Upper bound (inclusive), null = no upper bound", example = "7000000")
    Double max;

    @Schema(description = "Number of listings matching this bucket, given every other active filter", example = "128")
    long count;
}
