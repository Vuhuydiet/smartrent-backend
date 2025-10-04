package com.smartrent.dto.request;

import com.smartrent.infra.repository.ValidListingFilter;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ValidListingFilter
public class ListingFilterRequest {

    Long addressId;

    Long provinceId;

    Long districtId;

    Long streetId;

    // New: text-based address filters (case-insensitive contains)
    String provinceName;

    String districtName;

    String wardName;

    String streetName;

    // Free-text search on address.fullAddress
    String addressText;

    Long categoryId;

    @Min(0)
    Long priceMin;

    @Min(0)
    Long priceMax;

    @Min(0)
    Integer areaMin;

    @Min(0)
    Integer areaMax;

    List<Long> amenities;

    LocalDate createdAt;

    String status;

    Boolean verified;

    @Min(0)
    Integer bedrooms;

    String direction;

    @Min(0)
    Integer page = 0;

    @Min(1)
    Integer size = 20;
}
