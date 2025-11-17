package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Comprehensive address mapping from legacy (63 provinces) to new (34 provinces) structure
 * Maps to address_mapping table from V35 migration
 * Handles province merges, ward merges, and ward splits
 */
@Entity
@Table(name = "address_mapping",
        indexes = {
                @Index(name = "idx_mapping_legacy_province", columnList = "legacy_province_code"),
                @Index(name = "idx_mapping_legacy_district", columnList = "legacy_district_code"),
                @Index(name = "idx_mapping_legacy_ward", columnList = "legacy_ward_code"),
                @Index(name = "idx_mapping_new_province", columnList = "new_province_code"),
                @Index(name = "idx_mapping_new_ward", columnList = "new_ward_code"),
                @Index(name = "idx_mapping_merged_province", columnList = "is_merged_province"),
                @Index(name = "idx_mapping_merged_ward", columnList = "is_merged_ward"),
                @Index(name = "idx_mapping_divided_ward", columnList = "is_divided_ward"),
                @Index(name = "idx_mapping_default_ward", columnList = "is_default_new_ward"),
                @Index(name = "idx_mapping_full_legacy_address", columnList = "legacy_province_code, legacy_district_code, legacy_ward_code"),
                @Index(name = "idx_mapping_full_new_address", columnList = "new_province_code, new_ward_code")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    Integer id;

    // ========== Legacy Address (63 provinces) ==========
    @Column(name = "legacy_province_code", length = 10, nullable = false, insertable = false, updatable = false)
    String legacyProvinceCode;

    @Column(name = "legacy_district_code", length = 10, insertable = false, updatable = false)
    String legacyDistrictCode;

    @Column(name = "legacy_ward_code", length = 10, insertable = false, updatable = false)
    String legacyWardCode;

    @Column(name = "legacy_province_name", nullable = false)
    String legacyProvinceName;

    @Column(name = "legacy_district_name")
    String legacyDistrictName;

    @Column(name = "legacy_ward_name")
    String legacyWardName;

    @Column(name = "legacy_province_short")
    String legacyProvinceShort;

    @Column(name = "legacy_district_short")
    String legacyDistrictShort;

    @Column(name = "legacy_ward_short")
    String legacyWardShort;

    // ========== New Address (34 provinces) ==========
    @Column(name = "new_province_code", length = 10, nullable = false, insertable = false, updatable = false)
    String newProvinceCode;

    @Column(name = "new_ward_code", length = 10, insertable = false, updatable = false)
    String newWardCode;

    @Column(name = "new_province_name", nullable = false)
    String newProvinceName;

    @Column(name = "new_ward_name")
    String newWardName;

    @Column(name = "new_province_short")
    String newProvinceShort;

    @Column(name = "new_ward_short")
    String newWardShort;

    @Column(name = "new_ward_type", length = 50)
    String newWardType;

    // ========== Geographic Data ==========
    @Column(name = "legacy_province_lat", precision = 10, scale = 7)
    BigDecimal legacyProvinceLat;

    @Column(name = "legacy_province_lon", precision = 10, scale = 7)
    BigDecimal legacyProvinceLon;

    @Column(name = "legacy_district_lat", precision = 10, scale = 7)
    BigDecimal legacyDistrictLat;

    @Column(name = "legacy_district_lon", precision = 10, scale = 7)
    BigDecimal legacyDistrictLon;

    @Column(name = "legacy_ward_lat", precision = 10, scale = 7)
    BigDecimal legacyWardLat;

    @Column(name = "legacy_ward_lon", precision = 10, scale = 7)
    BigDecimal legacyWardLon;

    @Column(name = "new_province_lat", precision = 10, scale = 7)
    BigDecimal newProvinceLat;

    @Column(name = "new_province_lon", precision = 10, scale = 7)
    BigDecimal newProvinceLon;

    @Column(name = "new_ward_lat", precision = 10, scale = 7)
    BigDecimal newWardLat;

    @Column(name = "new_ward_lon", precision = 10, scale = 7)
    BigDecimal newWardLon;

    @Column(name = "new_ward_area_km2", precision = 10, scale = 2)
    BigDecimal newWardAreaKm2;

    // ========== Mapping Metadata Flags ==========
    @Column(name = "is_merged_province")
    Boolean isMergedProvince;

    @Column(name = "is_merged_ward")
    Boolean isMergedWard;

    @Column(name = "is_divided_ward")
    Boolean isDividedWard;

    @Column(name = "is_default_new_ward")
    Boolean isDefaultNewWard;

    @Column(name = "is_nearest_new_ward")
    Boolean isNearestNewWard;

    @Column(name = "is_new_ward_polygon_contains_ward")
    Boolean isNewWardPolygonContainsWard;

    // ========== Bounds Data ==========
    @Column(name = "legacy_province_bounds", columnDefinition = "TEXT")
    String legacyProvinceBounds;

    @Column(name = "legacy_district_bounds", columnDefinition = "TEXT")
    String legacyDistrictBounds;

    @Column(name = "legacy_ward_bounds", columnDefinition = "TEXT")
    String legacyWardBounds;

    // ========== Address Data ==========
    @Column(name = "legacy_province_geo_address", columnDefinition = "TEXT")
    String legacyProvinceGeoAddress;

    @Column(name = "legacy_district_geo_address", columnDefinition = "TEXT")
    String legacyDistrictGeoAddress;

    @Column(name = "legacy_ward_geo_address", columnDefinition = "TEXT")
    String legacyWardGeoAddress;

    // ========== Timestamps ==========
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // ========== Relationships ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legacy_province_code", referencedColumnName = "province_code", nullable = false)
    LegacyProvince legacyProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legacy_district_code", referencedColumnName = "district_code")
    District legacyDistrict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legacy_ward_code", referencedColumnName = "ward_code")
    LegacyWard legacyWard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_province_code", referencedColumnName = "province_code", nullable = false)
    Province newProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_ward_code", referencedColumnName = "ward_code")
    Ward newWard;
}
