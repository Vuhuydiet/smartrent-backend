package com.smartrent.infra.repository;

import com.smartrent.controller.dto.request.ListingFilterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ListingFilterValidator implements ConstraintValidator<ValidListingFilter, ListingFilterRequest> {
    @Override
    public boolean isValid(ListingFilterRequest value, ConstraintValidatorContext context) {
        if (value == null) return true;
        boolean valid = true;
        if (value.getPriceMin() != null && value.getPriceMax() != null) {
            valid &= value.getPriceMin() <= value.getPriceMax();
        }
        if (value.getAreaMin() != null && value.getAreaMax() != null) {
            valid &= value.getAreaMin() <= value.getAreaMax();
        }
        return valid;
    }
}
