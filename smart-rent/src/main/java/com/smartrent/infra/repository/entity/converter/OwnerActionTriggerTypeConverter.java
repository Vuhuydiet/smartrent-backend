package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.OwnerActionTriggerType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link OwnerActionTypeConverter}: a legacy/manual DB write can leave
 * {@code trigger_type} holding a value that isn't a current {@link OwnerActionTriggerType}
 * constant (e.g. {@code USER_REPORT}, which was never a declared value here). Without this
 * converter, Hibernate's default enum mapping throws {@link IllegalArgumentException} while
 * hydrating the row, which crashes every bulk read of owner actions — including
 * GET /v1/listings/my-listings, where one poisoned row blanks out an owner's entire listing
 * list. Fall back to REPORT_RESOLVED instead of throwing.
 */
@Slf4j
@Converter
public class OwnerActionTriggerTypeConverter implements AttributeConverter<OwnerActionTriggerType, String> {

    @Override
    public String convertToDatabaseColumn(OwnerActionTriggerType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public OwnerActionTriggerType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return OwnerActionTriggerType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown trigger_type value '{}' found in DB; defaulting to REPORT_RESOLVED", dbData);
            return OwnerActionTriggerType.REPORT_RESOLVED;
        }
    }
}
