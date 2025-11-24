package com.smartrent.dto.request;

import com.smartrent.infra.repository.entity.AddressMetadata;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * DTO for creating a new address
 * Supports both nested structure (legacy/new) and flat structure (backward compatibility)
 *
 * Nested structure example:
 * {
 *   "legacy": { "provinceId": 1, "districtId": 5, "wardId": 20, "street": "Nguyen Trai" },
 *   "latitude": 21.0285,
 *   "longitude": 105.8542
 * }
 *
 * OR
 *
 * {
 *   "new": { "provinceCode": "01", "wardCode": "00004", "street": "Nguyen Trai" },
 *   "latitude": 21.0285,
 *   "longitude": 105.8542
 * }
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressCreationRequest {

    // ==================== NESTED STRUCTURE (PREFERRED) ====================

    /**
     * Legacy address data (63 provinces, 3-tier structure)
     * Use this for old address format
     */
    @Valid
    LegacyAddressData legacy;

    /**
     * New address data (34 provinces, 2-tier structure)
     * Use this for new address format
     */
    @Valid
    NewAddressData newAddress;

    // ==================== FLAT STRUCTURE (BACKWARD COMPATIBILITY) ====================

    /**
     * Address type - for backward compatibility
     * Will be auto-detected from legacy/new fields if not provided
     */
    AddressMetadata.AddressType addressType;

    /**
     * Province ID for old structure (backward compatibility)
     * @deprecated Use legacy.provinceId instead
     */
    @Deprecated
    Integer provinceId;

    /**
     * District ID for old structure (backward compatibility)
     * @deprecated Use legacy.districtId instead
     */
    @Deprecated
    Integer districtId;

    /**
     * Ward ID for old structure (backward compatibility)
     * @deprecated Use legacy.wardId instead
     */
    @Deprecated
    Integer wardId;

    /**
     * Province code for new structure (backward compatibility)
     * @deprecated Use newAddress.provinceCode instead
     */
    @Deprecated
    String newProvinceCode;

    /**
     * Ward code for new structure (backward compatibility)
     * @deprecated Use newAddress.wardCode instead
     */
    @Deprecated
    String newWardCode;

    /**
     * Street ID (backward compatibility)
     * @deprecated Use legacy.street or newAddress.street instead
     */
    @Deprecated
    Integer streetId;

    /**
     * Project/Location ID (optional)
     */
    Integer projectId;

    /**
     * Street number (backward compatibility)
     * @deprecated Use legacy.streetNumber or newAddress.streetNumber instead
     */
    @Deprecated
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

    // ==================== HELPER METHODS ====================

    /**
     * Check if using legacy (old) address structure
     */
    public boolean isLegacyStructure() {
        // Check nested structure first
        if (legacy != null && legacy.isValid()) {
            return true;
        }
        // Fallback to flat structure for backward compatibility
        if (addressType == AddressMetadata.AddressType.OLD) {
            return true;
        }
        // Auto-detect from flat fields
        return provinceId != null && districtId != null && wardId != null;
    }

    /**
     * Check if using new address structure
     */
    public boolean isNewStructure() {
        // Check nested structure first
        if (newAddress != null && newAddress.isValid()) {
            return true;
        }
        // Fallback to flat structure for backward compatibility
        if (addressType == AddressMetadata.AddressType.NEW) {
            return true;
        }
        // Auto-detect from flat fields
        return newProvinceCode != null && !newProvinceCode.isEmpty() &&
               newWardCode != null && !newWardCode.isEmpty();
    }

    /**
     * Get the effective address type (auto-detect if not set)
     */
    public AddressMetadata.AddressType getEffectiveAddressType() {
        if (addressType != null) {
            return addressType;
        }
        return isLegacyStructure() ? AddressMetadata.AddressType.OLD : AddressMetadata.AddressType.NEW;
    }

    /**
     * Validate that required fields are present
     */
    public boolean hasRequiredFields() {
        // Check nested structure
        if (legacy != null && legacy.isValid()) {
            return true;
        }
        if (newAddress != null && newAddress.isValid()) {
            return true;
        }

        // Fallback to flat structure for backward compatibility
        if (isLegacyStructure()) {
            return provinceId != null && districtId != null && wardId != null;
        } else if (isNewStructure()) {
            return newProvinceCode != null && !newProvinceCode.isEmpty() &&
                   newWardCode != null && !newWardCode.isEmpty();
        }
        return false;
    }

    /**
     * Get legacy province ID (from nested or flat structure)
     */
    public Integer getLegacyProvinceId() {
        return legacy != null ? legacy.getProvinceId() : provinceId;
    }

    /**
     * Get legacy district ID (from nested or flat structure)
     */
    public Integer getLegacyDistrictId() {
        return legacy != null ? legacy.getDistrictId() : districtId;
    }

    /**
     * Get legacy ward ID (from nested or flat structure)
     */
    public Integer getLegacyWardId() {
        return legacy != null ? legacy.getWardId() : wardId;
    }

    /**
     * Get new province code (from nested or flat structure)
     */
    public String getNewProvinceCodeValue() {
        return newAddress != null ? newAddress.getProvinceCode() : newProvinceCode;
    }

    /**
     * Get new ward code (from nested or flat structure)
     */
    public String getNewWardCodeValue() {
        return newAddress != null ? newAddress.getWardCode() : newWardCode;
    }

    /**
     * Get street (from nested or flat structure)
     */
    public String getStreet() {
        if (legacy != null && legacy.getStreet() != null) {
            return legacy.getStreet();
        }
        if (newAddress != null && newAddress.getStreet() != null) {
            return newAddress.getStreet();
        }
        return null;
    }

}
