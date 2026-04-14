package com.smartrent.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Paginated response for public-facing listing card display.
 * Paired with ListingCardResponse — used by search and seller profile endpoints.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingCardListResponse {

    List<ListingCardResponse> listings;
    Long totalCount;
    Integer currentPage;
    Integer pageSize;
    Integer totalPages;
}
