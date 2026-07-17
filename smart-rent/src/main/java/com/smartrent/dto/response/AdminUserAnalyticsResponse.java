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
public class AdminUserAnalyticsResponse {

    List<TimeSeriesDataPoint> dataPoints;
    Long total;
    String granularity;

    List<TimeSeriesDataPoint> cumulativeDataPoints;
    Long totalUsersAsOfRangeEnd;

    List<CategoryBreakdownItem> roleBreakdown;
    List<CategoryBreakdownItem> brokerVerificationBreakdown;

    /**
     * Current, system-wide number of brokers awaiting approval (verification status PENDING),
     * independent of the selected date range. Backs the "brokers pending" KPI so it reflects the
     * live backlog rather than only brokers registered within the window.
     */
    Long brokersPendingApproval;
}
