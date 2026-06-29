package com.smartrent.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bounds on page/size so a bad client (e.g. the AI tool) gets a clear 400 via
 * @Valid on the controller instead of a runaway full-table scan.
 */
class ListingFilterRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsSizeAboveMax() {
        Set<ConstraintViolation<ListingFilterRequest>> v =
                validator.validate(ListingFilterRequest.builder().size(999).build());
        assertFalse(v.isEmpty());
    }

    @Test
    void rejectsPageBelowOne() {
        Set<ConstraintViolation<ListingFilterRequest>> v =
                validator.validate(ListingFilterRequest.builder().page(0).build());
        assertFalse(v.isEmpty());
    }

    @Test
    void acceptsInRangePageAndSize() {
        Set<ConstraintViolation<ListingFilterRequest>> v =
                validator.validate(ListingFilterRequest.builder().page(1).size(20).build());
        assertTrue(v.isEmpty());
    }
}
