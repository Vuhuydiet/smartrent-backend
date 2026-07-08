package com.smartrent.mapper.impl;

import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.ListingCardResponse;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.mapper.AmenityMapper;
import com.smartrent.mapper.MediaMapper;
import com.smartrent.infra.repository.LegacyDistrictRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * toCardResponse() feeds the /v1/listings/map-bounds response, which the map
 * page renders pins from. AddressCard used to declare only fullAddress/
 * fullNewAddress, so the map's latitude/longitude were silently dropped even
 * though the AddressResponse passed in had them -- every listing matched the
 * bounds query but rendered no pin. Locks in that address.latitude/longitude
 * survive the entity -> card mapping.
 */
@ExtendWith(MockitoExtension.class)
class ListingMapperImplTest {

    @Mock
    AmenityMapper amenityMapper;

    @Mock
    MediaMapper mediaMapper;

    @Mock
    LegacyDistrictRepository legacyDistrictRepository;

    @Test
    void toCardResponseKeepsAddressCoordinates() {
        ListingMapperImpl mapper =
                new ListingMapperImpl(amenityMapper, mediaMapper, legacyDistrictRepository);

        Listing listing = Listing.builder().listingId(1L).build();
        AddressResponse address = AddressResponse.builder()
                .fullAddress("123 Le Loi")
                .fullNewAddress("123 Le Loi, Da Nang")
                .latitude(BigDecimal.valueOf(16.0544))
                .longitude(BigDecimal.valueOf(108.2022))
                .build();

        ListingCardResponse card = mapper.toCardResponse(listing, null, address);

        assertEquals(BigDecimal.valueOf(16.0544), card.getAddress().getLatitude());
        assertEquals(BigDecimal.valueOf(108.2022), card.getAddress().getLongitude());
    }
}
