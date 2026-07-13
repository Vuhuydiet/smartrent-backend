package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Listing;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave a
 * price-unit column holding a value that isn't a current {@link Listing.PriceUnit} constant.
 * Fall back to MONTH (the field's existing Java default, and the overwhelmingly common case)
 * instead of letting Hibernate throw {@link IllegalArgumentException} while hydrating the row —
 * used by Listing.priceUnit and PricingHistory.oldPriceUnit/newPriceUnit, all of which reuse
 * this same nested enum type.
 */
@Slf4j
@Converter
public class ListingPriceUnitConverter implements AttributeConverter<Listing.PriceUnit, String> {

    @Override
    public String convertToDatabaseColumn(Listing.PriceUnit attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Listing.PriceUnit convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Listing.PriceUnit.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown price_unit value '{}' found in DB; defaulting to MONTH", dbData);
            return Listing.PriceUnit.MONTH;
        }
    }
}
