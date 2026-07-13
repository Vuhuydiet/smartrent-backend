package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Media;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MediaStatusConverterTest {

    private final MediaStatusConverter converter = new MediaStatusConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(Media.MediaStatus.ACTIVE, converter.convertToEntityAttribute("ACTIVE"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToArchivedInsteadOfThrowing() {
        assertEquals(Media.MediaStatus.ARCHIVED, converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
