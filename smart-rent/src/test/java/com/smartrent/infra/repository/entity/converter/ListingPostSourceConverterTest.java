package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.PostSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListingPostSourceConverterTest {

    private final ListingPostSourceConverter converter = new ListingPostSourceConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(PostSource.DIRECT_PAYMENT, converter.convertToEntityAttribute("DIRECT_PAYMENT"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToQuotaInsteadOfThrowing() {
        assertEquals(PostSource.QUOTA, converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
