package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Amenity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmenityCategoryConverterTest {

    private final AmenityCategoryConverter converter = new AmenityCategoryConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(Amenity.AmenityCategory.SECURITY, converter.convertToEntityAttribute("SECURITY"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToBasicInsteadOfThrowing() {
        assertEquals(Amenity.AmenityCategory.BASIC, converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
