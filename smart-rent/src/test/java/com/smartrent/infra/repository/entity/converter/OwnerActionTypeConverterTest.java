package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.OwnerActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OwnerActionTypeConverterTest {

    private final OwnerActionTypeConverter converter = new OwnerActionTypeConverter();

    @Test
    void convertsKnownValue() {
        assertEquals(OwnerActionType.EDIT_RESUBMIT, converter.convertToEntityAttribute("EDIT_RESUBMIT"));
    }

    @Test
    void nullPassesThrough() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void unknownLegacyValueFallsBackToUpdateListingInsteadOfThrowing() {
        // Regression: a stray listing_owner_actions.required_action row held a value with no
        // matching constant, which crashed GET /v1/listings/my-listings for that owner with
        // IllegalArgumentException on Hibernate hydration.
        assertEquals(OwnerActionType.UPDATE_LISTING, converter.convertToEntityAttribute("SOME_UNKNOWN_VALUE"));
    }
}
