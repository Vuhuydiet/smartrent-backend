package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.AddressMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AddressTypeConverterTest {

    private final AddressTypeConverter converter = new AddressTypeConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(AddressMetadata.AddressType.NEW, converter.convertToEntityAttribute("NEW"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToOldInsteadOfThrowing() {
        assertEquals(AddressMetadata.AddressType.OLD, converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
