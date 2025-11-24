package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Address entity - stores formatted address strings for both old and new structures
 * Matches database schema from V7 migration
 */
@Entity(name = "addresses")
@Table(name = "addresses")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    Long addressId;

    // ==================== FORMATTED ADDRESS STRINGS ====================

    /**
     * Formatted full address for old structure (63 provinces, 3-tier)
     * Example: "123 Đường Nguyễn Trãi, Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội"
     */
    @Column(name = "full_address", columnDefinition = "TEXT")
    String fullAddress;

    /**
     * Formatted full address for new structure (34 provinces, 2-tier)
     * Example: "123 Đường Nguyễn Trãi, Phường Ba Đình, Thành phố Hà Nội"
     */
    @Column(name = "full_newaddress", columnDefinition = "TEXT")
    String fullNewAddress;

    // ==================== LEGACY ADDRESS COMPONENTS (63 provinces) ====================

    /**
     * Legacy province ID (63 provinces structure)
     * Example: 1 for Hà Nội, 79 for TP.HCM
     */
    @Column(name = "legacy_province_id")
    Integer legacyProvinceId;

    /**
     * Legacy district ID
     * Example: 5 for Quận Ba Đình
     */
    @Column(name = "legacy_district_id")
    Integer legacyDistrictId;

    /**
     * Legacy ward ID
     * Example: 20 for Phường Điện Biên
     */
    @Column(name = "legacy_ward_id")
    Integer legacyWardId;

    /**
     * Legacy street name
     * Example: "Nguyễn Trãi"
     */
    @Column(name = "legacy_street")
    String legacyStreet;


    // ==================== NEW ADDRESS COMPONENTS (34 provinces) ====================

    /**
     * New province code (34 provinces structure)
     * Example: "01" for Hà Nội, "79" for TP.HCM
     */
    @Column(name = "new_province_code", length = 10)
    String newProvinceCode;

    /**
     * New ward code (2-tier structure, includes district info)
     * Example: "00004" for Phường Ba Đình, Hà Nội
     */
    @Column(name = "new_ward_code", length = 10)
    String newWardCode;

    /**
     * New street name
     * Example: "Nguyễn Trãi"
     */
    @Column(name = "new_street")
    String newStreet;

    /**

    // ==================== COMMON FIELDS ====================

    /**
     * Address structure type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false)
    AddressMetadata.AddressType addressType = AddressMetadata.AddressType.OLD;

    /**
     * Project/building/complex ID (optional)
     * References a project or building complex
     */
    @Column(name = "project_id")
    Integer projectId;

    // ==================== COORDINATES ====================

    @Column(name = "latitude", precision = 10, scale = 8)
    BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    BigDecimal longitude;

    // ==================== RELATIONSHIPS ====================

    @OneToMany(mappedBy = "address", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Listing> listings;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // ==================== HELPER METHODS ====================

    /**
     * Get the appropriate address display based on which format is available
     */
    public String getDisplayAddress() {
        if (addressType == AddressMetadata.AddressType.NEW && fullNewAddress != null && !fullNewAddress.isEmpty()) {
            return fullNewAddress;
        }
        if (fullAddress != null && !fullAddress.isEmpty()) {
            return fullAddress;
        }
        // Fallback: construct from components if full addresses are empty
        return buildAddressFromComponents();
    }

    /**
     * Check if this address uses the new structure
     */
    public boolean isNewStructure() {
        return addressType == AddressMetadata.AddressType.NEW;
    }

    /**
     * Check if this address uses the legacy structure
     */
    public boolean isLegacyStructure() {
        return addressType == AddressMetadata.AddressType.OLD;
    }

    /**
     * Check if legacy components are populated
     */
    public boolean hasLegacyComponents() {
        return legacyProvinceId != null || legacyDistrictId != null || legacyWardId != null;
    }

    /**
     * Check if new components are populated
     */
    public boolean hasNewComponents() {
        return (newProvinceCode != null && !newProvinceCode.isEmpty()) ||
               (newWardCode != null && !newWardCode.isEmpty());
    }

    /**
     * Build address string from components (fallback if full addresses are empty)
     */
    private String buildAddressFromComponents() {
        StringBuilder sb = new StringBuilder();

        String result = sb.toString().trim();
        return result.isEmpty() ? "Address not available" : result;
    }

    /**
     * Get street name based on address type
     */
    public String getStreet() {
        return isNewStructure() ? newStreet : legacyStreet;
    }
}
