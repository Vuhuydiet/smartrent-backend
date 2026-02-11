package com.smartrent.dto.response;

import com.smartrent.infra.repository.entity.Listing;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingAutocompleteResponse {
    Long listingId;
    String title;
    String address;
    BigDecimal price;
    Listing.PriceUnit priceUnit;
    Listing.VipType vipType;
}
