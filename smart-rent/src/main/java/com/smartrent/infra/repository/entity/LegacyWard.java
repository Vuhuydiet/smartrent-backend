package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Legacy ward structure (before July 1, 2025)
 * Maps to legacy_wards table from V34 migration
 * Contains denormalized province and district information
 */
@Entity
@Table(name = "legacy_wards",
        indexes = {
                @Index(name = "idx_legacy_wards_code", columnList = "ward_code"),
                @Index(name = "idx_legacy_wards_province_code", columnList = "province_code"),
                @Index(name = "idx_legacy_wards_district_code", columnList = "district_code"),
                @Index(name = "idx_legacy_wards_key", columnList = "ward_key"),
                @Index(name = "idx_legacy_wards_full_address", columnList = "province_code, district_code, ward_code")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyWard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_ward_id")
    Integer id;

    @Column(name = "ward_code", length = 10, nullable = false, unique = true)
    String code;

    @Column(name = "province_code", length = 10, nullable = false, insertable = false, updatable = false)
    String provinceCode;

    @Column(name = "district_code", length = 10, nullable = false, insertable = false, updatable = false)
    String districtCode;

    @Column(name = "ward_name", nullable = false)
    String name;

    @Column(name = "ward_short_name", nullable = false)
    String shortName;

    @Column(name = "ward_type", length = 50, nullable = false)
    String type;

    @Column(name = "ward_key")
    String key;

    @Column(name = "ward_latitude", precision = 10, scale = 7)
    BigDecimal latitude;

    @Column(name = "ward_longitude", precision = 10, scale = 7)
    BigDecimal longitude;

    @Column(name = "ward_bounds", columnDefinition = "TEXT")
    String bounds;

    @Column(name = "ward_geo_address", columnDefinition = "TEXT")
    String geoAddress;

    @Column(name = "ward_short_duplicated")
    Boolean shortDuplicated;

    // District info (denormalized)
    @Column(name = "district_name", nullable = false)
    String districtName;

    @Column(name = "district_short_name", nullable = false)
    String districtShortName;

    @Column(name = "district_type", length = 50, nullable = false)
    String districtType;

    @Column(name = "district_key")
    String districtKey;

    @Column(name = "district_latitude", precision = 10, scale = 7)
    BigDecimal districtLatitude;

    @Column(name = "district_longitude", precision = 10, scale = 7)
    BigDecimal districtLongitude;

    @Column(name = "district_bounds", columnDefinition = "TEXT")
    String districtBounds;

    @Column(name = "district_geo_address", columnDefinition = "TEXT")
    String districtGeoAddress;

    @Column(name = "district_short_duplicated")
    Boolean districtShortDuplicated;

    // Province info (denormalized)
    @Column(name = "province_name", nullable = false)
    String provinceName;

    @Column(name = "province_short_name", nullable = false)
    String provinceShortName;

    @Column(name = "province_key")
    String provinceKey;

    @Column(name = "province_latitude", precision = 10, scale = 7)
    BigDecimal provinceLatitude;

    @Column(name = "province_longitude", precision = 10, scale = 7)
    BigDecimal provinceLongitude;

    @Column(name = "province_bounds", columnDefinition = "TEXT")
    String provinceBounds;

    @Column(name = "province_geo_address", columnDefinition = "TEXT")
    String provinceGeoAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationships - reference by code instead of ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code", referencedColumnName = "province_code", nullable = false, insertable = false, updatable = false)
    LegacyProvince province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_code", referencedColumnName = "district_code", nullable = false)
    District district;
}
