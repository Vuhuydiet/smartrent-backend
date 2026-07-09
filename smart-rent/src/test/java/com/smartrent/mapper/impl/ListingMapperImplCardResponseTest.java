package com.smartrent.mapper.impl;

import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.ListingCardResponse;
import com.smartrent.infra.repository.entity.Listing;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The /map-bounds map view places every pin from listing.address.latitude /
 * listing.address.longitude and filters out any listing missing them. So
 * toCardResponse MUST copy the coordinates from the source AddressResponse onto
 * the card's AddressCard -- dropping them renders an empty map (0 pins, "0 bất
 * động sản") even on a successful HTTP 200. This locks that in.
 *
 * toCardResponse maps plain fields only (no injected collaborators are touched
 * on this path), so the mapper is constructed with null dependencies.
 */
class ListingMapperImplCardResponseTest {

    private final ListingMapperImpl mapper = new ListingMapperImpl(null, null, null);

    @Test
    void toCardResponseCopiesAddressCoordinates() {
        Listing listing = Listing.builder().listingId(1L).build();
        AddressResponse address = AddressResponse.builder()
                .fullNewAddress("123 Nguyễn Trãi, Thành Phố Hồ Chí Minh")
                .fullAddress("123 Nguyễn Trãi, Phường 5, Quận 5, TP.HCM")
                .latitude(new BigDecimal("10.75450000"))
                .longitude(new BigDecimal("106.66790000"))
                .build();

        ListingCardResponse card = mapper.toCardResponse(listing, null, address);

        assertNotNull(card.getAddress(), "map card must carry an address object");
        assertEquals(new BigDecimal("10.75450000"), card.getAddress().getLatitude(),
                "latitude must reach the map card so the FE can place the pin");
        assertEquals(new BigDecimal("106.66790000"), card.getAddress().getLongitude(),
                "longitude must reach the map card so the FE can place the pin");
    }
}
