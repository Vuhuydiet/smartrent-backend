package com.smartrent.dto.request;

import com.smartrent.infra.repository.entity.AddressMetadata;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * DTO for creating a new address using nested structure
 *
 * You can provide EITHER legacy OR new address structure, or BOTH.
 * If both are provided, both will be saved to the database.
 *
 * Legacy structure example (63 provinces, 3-tier):
 * {
 *   "legacy": {
 *     "provinceId": 1,
 *     "districtId": 5,
 *     "wardId": 20,
 *     "street": "Nguyen Trai"
 *   },
 *   "latitude": 21.0285,
 *   "longitude": 105.8542
 * }
 *
 * New structure example (34 provinces, 2-tier):
 * {
 *   "newAddress": {
 *     "provinceCode": "01",
 *     "wardCode": "00004",
 *     "street": "Nguyen Trai"
 *   },
 *   "latitude": 21.0285,
 *   "longitude": 105.8542
 * }
 *
 * Both structures example:
 * {
 *   "legacy": { "provinceId": 1, "districtId": 5, "wardId": 20, "street": "Nguyen Trai" },
 *   "newAddress": { "provinceCode": "01", "wardCode": "00004", "street": "Nguyen Trai" },
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
@Schema(description = "Address creation request with nested structure")
public class AddressCreationRequest {

    /**
     * Legacy address data (63 provinces, 3-tier structure)
     * Use this for old address format
     */
    @Valid
    @Schema(description = "Legacy address data (63 provinces, 3-tier: province/district/ward)")
    LegacyAddressData legacy;

    /**
     * New address data (34 provinces, 2-tier structure)
     * Use this for new address format
     */
    @Valid
    @Schema(description = "New address data (34 provinces, 2-tier: province/ward)")
    NewAddressData newAddress;

    /**
     * Project/Building/Complex ID (optional)
     * Reference to a specific building or complex
     */
    @Schema(description = "Project/building/complex ID (optional)", example = "1")
    Integer projectId;

    // ==================== COORDINATES ====================

    /**
     * Latitude coordinate
     * Vietnam range: approximately 8.0 to 23.5
     */
    @DecimalMin(value = "8.0", message = "Latitude must be at least 8.0")
    @DecimalMax(value = "23.5", message = "Latitude must be at most 23.5")
    @Schema(description = "Latitude coordinate (Vietnam: 8.0 to 23.5)", example = "21.0285")
    BigDecimal latitude;

    /**
     * Longitude coordinate
     * Vietnam range: approximately 102.0 to 110.0
     */
    @DecimalMin(value = "102.0", message = "Longitude must be at least 102.0")
    @DecimalMax(value = "110.0", message = "Longitude must be at most 110.0")
    @Schema(description = "Longitude coordinate (Vietnam: 102.0 to 110.0)", example = "105.8542")
    BigDecimal longitude;

    // ==================== HELPER METHODS ====================

    /**
     * Check if using legacy (old) address structure
     */
    public boolean isLegacyStructure() {
        return legacy != null && legacy.isValid();
    }

    /**
     * Check if using new address structure
     */
    public boolean isNewStructure() {
        return newAddress != null && newAddress.isValid();
    }

    /**
     * Get the effective address type (auto-detect from structure)
     * If both legacy and new structures are provided, prioritizes legacy (OLD) for addressType.
     * Note: Both structures will still be saved to the database if provided.
     */
    public AddressMetadata.AddressType getEffectiveAddressType() {
        boolean hasLegacy = isLegacyStructure();
        boolean hasNew = isNewStructure();

        if (!hasLegacy && !hasNew) {
            throw new IllegalStateException("Address structure not specified. Provide either 'legacy' or 'newAddress'");
        }

        // Priority: Legacy (OLD) > New (NEW) when both are provided
        return hasLegacy ? AddressMetadata.AddressType.OLD : AddressMetadata.AddressType.NEW;
    }

    /**
     * Validate that required fields are present
     */
    public boolean hasRequiredFields() {
        return isLegacyStructure() || isNewStructure();
    }

    /**
     * Get legacy province ID
     */
    public Integer getLegacyProvinceId() {
        return legacy != null ? legacy.getProvinceId() : null;
    }

    /**
     * Get legacy district ID
     */
    public Integer getLegacyDistrictId() {
        return legacy != null ? legacy.getDistrictId() : null;
    }

    /**
     * Get legacy ward ID
     */
    public Integer getLegacyWardId() {
        return legacy != null ? legacy.getWardId() : null;
    }

    /**
     * Get new province code
     */
    public String getNewProvinceCodeValue() {
        return newAddress != null ? newAddress.getProvinceCode() : null;
    }

    /**
     * Get new ward code
     */
    public String getNewWardCodeValue() {
        return newAddress != null ? newAddress.getWardCode() : null;
    }

    /**
     * Get street name
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
