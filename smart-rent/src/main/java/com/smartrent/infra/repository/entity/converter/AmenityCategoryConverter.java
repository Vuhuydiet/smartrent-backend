package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Amenity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code category} holding a value that isn't a current {@link Amenity.AmenityCategory}
 * constant. Fall back to BASIC instead of letting Hibernate throw
 * {@link IllegalArgumentException} while hydrating the row.
 */
@Slf4j
@Converter
public class AmenityCategoryConverter implements AttributeConverter<Amenity.AmenityCategory, String> {

    @Override
    public String convertToDatabaseColumn(Amenity.AmenityCategory attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Amenity.AmenityCategory convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Amenity.AmenityCategory.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown amenity category value '{}' found in DB; defaulting to BASIC", dbData);
            return Amenity.AmenityCategory.BASIC;
        }
    }
}
