package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevenueOverTimeResponse {

    List<RevenueDataPoint> dataPoints;
    BigDecimal grandTotal;
    Long totalTransactions;
    List<RevenueByTypeItem> revenueByType;
}
