package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Listing;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code furnishing} holding a value that isn't a current {@link Listing.Furnishing} constant.
 * The column is nullable and purely descriptive, so fall back to null (just don't show a
 * furnishing badge) instead of letting Hibernate throw {@link IllegalArgumentException} while
 * hydrating the row.
 */
@Slf4j
@Converter
public class ListingFurnishingConverter implements AttributeConverter<Listing.Furnishing, String> {

    @Override
    public String convertToDatabaseColumn(Listing.Furnishing attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Listing.Furnishing convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Listing.Furnishing.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown furnishing value '{}' found in DB; defaulting to null", dbData);
            return null;
        }
    }
}
