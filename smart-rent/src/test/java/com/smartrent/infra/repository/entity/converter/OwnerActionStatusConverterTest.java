package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.OwnerActionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OwnerActionStatusConverterTest {

    private final OwnerActionStatusConverter converter = new OwnerActionStatusConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(OwnerActionStatus.COMPLETED, converter.convertToEntityAttribute("COMPLETED"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToPendingOwnerInsteadOfThrowing() {
        assertEquals(OwnerActionStatus.PENDING_OWNER, converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
