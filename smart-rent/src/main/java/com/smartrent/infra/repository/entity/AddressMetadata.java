package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * AddressMetadata - stores structured address component references
 * Allows querying listings by province, district, ward, etc.
 */
@Entity
@Table(name = "address_metadata",
        indexes = {
                @Index(name = "idx_address_type", columnList = "address_type"),
                @Index(name = "idx_old_province", columnList = "province_id"),
                @Index(name = "idx_old_district", columnList = "district_id"),
                @Index(name = "idx_old_ward", columnList = "ward_id"),
                @Index(name = "idx_new_province", columnList = "new_province_code"),
                @Index(name = "idx_new_ward", columnList = "new_ward_code"),
                @Index(name = "idx_street", columnList = "street_id"),
                @Index(name = "idx_project", columnList = "project_id")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metadata_id")
    Long metadataId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    Address address;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 10)
    AddressType addressType;

    // Old address structure (63 provinces, 3-tier)
    @Column(name = "province_id")
    Integer provinceId;

    @Column(name = "district_id")
    Integer districtId;

    @Column(name = "ward_id")
    Integer wardId;

    // New address structure (34 provinces, 2-tier)
    @Column(name = "new_province_code", length = 10)
    String newProvinceCode;

    @Column(name = "new_ward_code", length = 10)
    String newWardCode;

    // Common fields
    @Column(name = "street_id")
    Integer streetId;

    @Column(name = "project_id")
    Integer projectId;

    @Column(name = "street_number", length = 20)
    String streetNumber;

    public enum AddressType {
        OLD, NEW
    }

    /**
     * Check if this is using old address structure
     */
    public boolean isOldStructure() {
        return addressType == AddressType.OLD;
    }

    /**
     * Check if this is using new address structure
     */
    public boolean isNewStructure() {
        return addressType == AddressType.NEW;
    }
}
