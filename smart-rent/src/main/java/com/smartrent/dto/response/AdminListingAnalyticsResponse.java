package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminListingAnalyticsResponse {

    List<TimeSeriesDataPoint> dataPoints;
    Long total;
    String granularity;

    List<TimeSeriesDataPoint> cumulativeDataPoints;
    Long totalListingsAsOfRangeEnd;

    List<CategoryBreakdownItem> listingTypeBreakdown;
    List<CategoryBreakdownItem> productTypeBreakdown;
    List<CategoryBreakdownItem> verificationBreakdown;
}
