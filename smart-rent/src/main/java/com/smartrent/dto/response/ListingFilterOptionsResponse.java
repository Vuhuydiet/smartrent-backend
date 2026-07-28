package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Dynamic filter bucket options for the public listings sidebar "
        + "(price / area / bedrooms), each annotated with a live count under the current filter context")
public class ListingFilterOptionsResponse {

    List<FilterBucketOption> priceOptions;

    List<FilterBucketOption> areaOptions;

    List<FilterBucketOption> bedroomOptions;
}
