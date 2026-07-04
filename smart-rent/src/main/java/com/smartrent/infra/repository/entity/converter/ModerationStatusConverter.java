package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.ModerationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * A legacy/manual DB write can leave {@code moderation_status} holding a value that isn't a
 * {@link ModerationStatus} constant (e.g. the display-only {@code ListingStatus.IN_REVIEW} name
 * used instead of {@code ModerationStatus.PENDING_REVIEW}). Without this converter, Hibernate's
 * default enum mapping throws {@link IllegalArgumentException} while hydrating the row, which
 * crashes every code path that reads that listing. Fall back to PENDING_REVIEW instead so a
 * single bad row degrades to "needs review" rather than a 500.
 */
@Slf4j
@Converter
public class ModerationStatusConverter implements AttributeConverter<ModerationStatus, String> {

    @Override
    public String convertToDatabaseColumn(ModerationStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ModerationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return ModerationStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown moderation_status value '{}' found in DB; defaulting to PENDING_REVIEW", dbData);
            return ModerationStatus.PENDING_REVIEW;
        }
    }
}
