package com.smartrent.dto.request;

import com.smartrent.infra.repository.entity.AddressMetadata;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * DTO for creating a new address
 * Supports both old (63 provinces, 3-tier) and new (34 provinces, 2-tier) address structures
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressCreationRequest {

    @NotNull(message = "Address type is required")
    AddressMetadata.AddressType addressType;

    // ==================== OLD ADDRESS STRUCTURE (63 provinces) ====================

    /**
     * Province ID for old structure (required if addressType = OLD)
     */
    Integer provinceId;

    /**
     * District ID for old structure (required if addressType = OLD)
     */
    Integer districtId;

    /**
     * Ward ID for old structure (required if addressType = OLD)
     */
    Integer wardId;

    // ==================== NEW ADDRESS STRUCTURE (34 provinces) ====================

    /**
     * Province code for new structure (required if addressType = NEW)
     * Example: "01" for Hà Nội
     */
    String newProvinceCode;

    /**
     * Ward code for new structure (required if addressType = NEW)
     * Example: "00004" for Ba Đình ward
     */
    String newWardCode;

    // ==================== COMMON FIELDS ====================

    /**
     * Street ID (optional)
     * Can be used for both old and new structures
     */
    Integer streetId;

    /**
     * Project/Location ID (optional)
     * Alternative to street, used for buildings/complexes
     */
    Integer projectId;

    /**
     * Street number/house number (optional)
     * Example: "123", "45A", "123/45"
     */
    String streetNumber;

    // ==================== COORDINATES ====================

    /**
     * Latitude coordinate
     * Vietnam range: approximately 8.0 to 23.5
     */
    @DecimalMin(value = "8.0", message = "Latitude must be at least 8.0")
    @DecimalMax(value = "23.5", message = "Latitude must be at most 23.5")
    BigDecimal latitude;

    /**
     * Longitude coordinate
     * Vietnam range: approximately 102.0 to 110.0
     */
    @DecimalMin(value = "102.0", message = "Longitude must be at least 102.0")
    @DecimalMax(value = "110.0", message = "Longitude must be at most 110.0")
    BigDecimal longitude;

    /**
     * Check if this is using old address structure
     */
    public boolean isOldStructure() {
        return addressType == AddressMetadata.AddressType.OLD;
    }

    /**
     * Check if this is using new address structure
     */
    public boolean isNewStructure() {
        return addressType == AddressMetadata.AddressType.NEW;
    }

    /**
     * Validate that required fields are present based on address type
     */
    public boolean hasRequiredFields() {
        if (isOldStructure()) {
            return provinceId != null && districtId != null && wardId != null;
        } else if (isNewStructure()) {
            return newProvinceCode != null && !newProvinceCode.isEmpty() &&
                   newWardCode != null && !newWardCode.isEmpty();
        }
        return false;
    }
}
