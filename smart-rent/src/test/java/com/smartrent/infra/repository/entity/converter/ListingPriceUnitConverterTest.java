package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Listing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListingPriceUnitConverterTest {

    private final ListingPriceUnitConverter converter = new ListingPriceUnitConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(Listing.PriceUnit.DAY, converter.convertToEntityAttribute("DAY"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToMonthInsteadOfThrowing() {
        assertEquals(Listing.PriceUnit.MONTH, converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
