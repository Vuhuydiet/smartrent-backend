package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Listing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListingVipTypeConverterTest {

    private final ListingVipTypeConverter converter = new ListingVipTypeConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(Listing.VipType.GOLD, converter.convertToEntityAttribute("GOLD"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToNormalInsteadOfThrowing() {
        assertEquals(Listing.VipType.NORMAL, converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
