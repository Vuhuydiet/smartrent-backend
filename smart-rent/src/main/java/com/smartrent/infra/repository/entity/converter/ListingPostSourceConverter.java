package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.PostSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code post_source} holding a value that isn't a current {@link PostSource} constant. Fall
 * back to QUOTA (the field's existing Java default) instead of letting Hibernate throw
 * {@link IllegalArgumentException} while hydrating the row.
 */
@Slf4j
@Converter
public class ListingPostSourceConverter implements AttributeConverter<PostSource, String> {

    @Override
    public String convertToDatabaseColumn(PostSource attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PostSource convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return PostSource.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown post_source value '{}' found in DB; defaulting to QUOTA", dbData);
            return PostSource.QUOTA;
        }
    }
}
