package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.Listing;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code vip_type} holding a value that isn't a current {@link Listing.VipType} constant. Fall
 * back to NORMAL (the field's existing Java default) instead of letting Hibernate throw
 * {@link IllegalArgumentException} while hydrating the row — this degrades a listing to the
 * non-promoted tier rather than crashing every bulk listing query.
 */
@Slf4j
@Converter
public class ListingVipTypeConverter implements AttributeConverter<Listing.VipType, String> {

    @Override
    public String convertToDatabaseColumn(Listing.VipType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Listing.VipType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Listing.VipType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown vip_type value '{}' found in DB; defaulting to NORMAL", dbData);
            return Listing.VipType.NORMAL;
        }
    }
}
