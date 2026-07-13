package com.smartrent.infra.repository.entity.converter;

import com.smartrent.enums.OwnerActionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code required_action} holding a value that isn't a current {@link OwnerActionType} constant.
 * Without this converter, Hibernate's default enum mapping throws {@link IllegalArgumentException}
 * while hydrating the row, which crashes every code path that reads it — including bulk reads
 * like GET /v1/listings/my-listings, where one poisoned row blanks out an owner's entire listing
 * list. Fall back to UPDATE_LISTING (the most conservative "something needs the owner's attention"
 * value) instead of throwing.
 */
@Slf4j
@Converter
public class OwnerActionTypeConverter implements AttributeConverter<OwnerActionType, String> {

    @Override
    public String convertToDatabaseColumn(OwnerActionType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public OwnerActionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return OwnerActionType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown required_action value '{}' found in DB; defaulting to UPDATE_LISTING", dbData);
            return OwnerActionType.UPDATE_LISTING;
        }
    }
}
