package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Media;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code status} holding a value that isn't a current {@link Media.MediaStatus} constant.
 * Fall back to ARCHIVED (soft-hide) instead of letting Hibernate throw
 * {@link IllegalArgumentException} while hydrating the row — this column is read via
 * bulk/IN-clause queries (e.g. findActiveMediaByListingIds), so one bad row could otherwise
 * blank out media for every listing in the same batch.
 */
@Slf4j
@Converter
public class MediaStatusConverter implements AttributeConverter<Media.MediaStatus, String> {

    @Override
    public String convertToDatabaseColumn(Media.MediaStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Media.MediaStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Media.MediaStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown media status value '{}' found in DB; defaulting to ARCHIVED", dbData);
            return Media.MediaStatus.ARCHIVED;
        }
    }
}
