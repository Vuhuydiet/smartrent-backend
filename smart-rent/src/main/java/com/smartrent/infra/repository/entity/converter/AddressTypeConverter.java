package com.smartrent.infra.repository.entity.converter;

import com.smartrent.infra.repository.entity.AddressMetadata;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Same rationale as {@link ModerationStatusConverter}: a legacy/manual DB write can leave
 * {@code address_type} holding a value that isn't a current {@link AddressMetadata.AddressType}
 * constant. Fall back to OLD (the field's existing Java default) instead of letting Hibernate
 * throw {@link IllegalArgumentException} while hydrating the row — used by both
 * AddressMetadata.addressType and Address.addressType.
 */
@Slf4j
@Converter
public class AddressTypeConverter implements AttributeConverter<AddressMetadata.AddressType, String> {

    @Override
    public String convertToDatabaseColumn(AddressMetadata.AddressType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public AddressMetadata.AddressType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return AddressMetadata.AddressType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.error("Unknown address_type value '{}' found in DB; defaulting to OLD", dbData);
            return AddressMetadata.AddressType.OLD;
        }
    }
}
