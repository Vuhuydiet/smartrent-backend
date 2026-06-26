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
 * Response for {@code POST /v1/listings/search/sections}.
 * One {@link SectionResult} per requested tier, in request order. Each result is
 * a plain {@link ListingCardListResponse}, identical to what {@code POST /search}
 * returns for that tier — so the frontend mapping is unchanged.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingSectionsResponse {

    List<SectionResult> sections;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SectionResult {

        String vipType;
        String sortBy;
        ListingCardListResponse result;
    }
}
