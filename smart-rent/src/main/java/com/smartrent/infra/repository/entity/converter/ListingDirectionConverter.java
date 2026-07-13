package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Listing;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code direction} holding a value that isn't a current {@link Listing.Direction} constant.
 * The column is nullable and purely descriptive (compass direction), so fall back to null
 * (just don't show a direction) instead of letting Hibernate throw
 * {@link IllegalArgumentException} while hydrating the row.
 */
@Slf4j
@Converter
public class ListingDirectionConverter implements AttributeConverter<Listing.Direction, String> {

    @Override
    public String convertToDatabaseColumn(Listing.Direction attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Listing.Direction convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Listing.Direction.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown direction value '{}' found in DB; defaulting to null", dbData);
            return null;
        }
    }
}
