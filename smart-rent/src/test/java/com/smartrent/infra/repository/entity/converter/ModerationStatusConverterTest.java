package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.ModerationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModerationStatusConverterTest {

    private final ModerationStatusConverter converter = new ModerationStatusConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(ModerationStatus.APPROVED, converter.convertToEntityAttribute("APPROVED"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToPendingReviewInsteadOfThrowing() {
        // Regression: a stray DB row held "IN_REVIEW" (the ListingStatus display name, not a
        // ModerationStatus constant), which used to crash Hibernate hydration with
        // IllegalArgumentException on every read (report flow, listing detail, etc).
        assertEquals(ModerationStatus.PENDING_REVIEW, converter.convertToEntityAttribute("IN_REVIEW"));
    }
}
