package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.OwnerActionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code status} holding a value that isn't a current {@link OwnerActionStatus} constant.
 * This column is read in the same bulk query (findByListingIdInAndStatus) that crashed
 * GET /v1/listings/my-listings on an unrecognized {@code required_action} value — fall back
 * to PENDING_OWNER instead of throwing so one bad row can't blank out an owner's whole list.
 */
@Slf4j
@Converter
public class OwnerActionStatusConverter implements AttributeConverter<OwnerActionStatus, String> {

    @Override
    public String convertToDatabaseColumn(OwnerActionStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public OwnerActionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return OwnerActionStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown owner action status value '{}' found in DB; defaulting to PENDING_OWNER", dbData);
            return OwnerActionStatus.PENDING_OWNER;
        }
    }
}
