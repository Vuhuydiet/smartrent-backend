package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Listing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListingFurnishingConverterTest {

    private final ListingFurnishingConverter converter = new ListingFurnishingConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(Listing.Furnishing.FULLY_FURNISHED, converter.convertToEntityAttribute("FULLY_FURNISHED"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToNullInsteadOfThrowing() {
        assertNull(converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
