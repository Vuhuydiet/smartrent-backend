package com.smartrent.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParsedCriteriaDto {
    private String propertyType; // "ROOM", "APARTMENT", etc. Matches Listing.ProductType or Listing.ListingType
    private String listingType;  // "RENT", "SALE"
    private String district;
    private String ward;
    private String province;
    private BigDecimal maxPrice;
    private BigDecimal minPrice;
    private Float minArea;       // m²
    private Float maxArea;       // m²
    private Integer bedrooms;    // minimum number of bedrooms
    private List<String> amenities;
    private String keyword; // For FULLTEXT search fallback
    private String phoneticKeyword; // For typo tolerance
}
