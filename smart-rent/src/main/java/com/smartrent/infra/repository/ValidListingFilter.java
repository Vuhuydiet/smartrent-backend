package com.smartrent.infra.repository;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ListingFilterValidator.class)
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ValidListingFilter {
    String message() default "Invalid filter: priceMin must be <= priceMax, areaMin must be <= areaMax";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
