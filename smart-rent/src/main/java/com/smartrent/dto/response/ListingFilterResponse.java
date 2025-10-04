package com.smartrent.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ListingFilterResponse {
    private List<ListingResponse> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
