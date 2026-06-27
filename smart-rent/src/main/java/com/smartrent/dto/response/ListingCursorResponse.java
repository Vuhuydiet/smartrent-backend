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
 * Cursor-paginated listing feed (POST /v1/listings/search/cursor).
 * Same card shape as the offset search, but paged by an opaque {@code nextCursor}
 * instead of page numbers — and with NO total count.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingCursorResponse {

    List<ListingCardResponse> items;

    /** Opaque token for the next page; {@code null} when there are no more rows. */
    String nextCursor;

    boolean hasNext;

    /** Echoes the effective page size used. */
    Integer size;
}
