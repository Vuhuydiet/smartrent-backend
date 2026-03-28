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
public class OwnerSavedListingsAnalyticsResponse {

    List<ListingSaveSummary> listings;
    Long totalSavesAcrossAll;

    // Pagination metadata
    Integer currentPage;
    Integer totalPages;
    Long totalElements;
    Integer pageSize;
}
