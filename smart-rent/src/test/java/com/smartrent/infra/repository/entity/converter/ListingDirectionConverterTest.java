package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Listing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListingDirectionConverterTest {

    private final ListingDirectionConverter converter = new ListingDirectionConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(Listing.Direction.NORTH, converter.convertToEntityAttribute("NORTH"));
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
