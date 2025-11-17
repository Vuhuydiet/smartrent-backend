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
 * Legacy district structure (before July 1, 2025)
 * Maps to legacy_districts table from V33 migration
 * Districts were eliminated in the new administrative structure
 */
@Entity
@Table(name = "legacy_districts",
        indexes = {
                @Index(name = "idx_legacy_districts_code", columnList = "district_code"),
                @Index(name = "idx_legacy_districts_province_code", columnList = "province_code"),
                @Index(name = "idx_legacy_districts_key", columnList = "district_key")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_district_id")
    Integer id;

    @Column(name = "district_code", length = 10, nullable = false, unique = true)
    String code;

    @Column(name = "province_code", length = 10, nullable = false, insertable = false, updatable = false)
    String provinceCode;

    @Column(name = "district_name", nullable = false)
    String name;

    @Column(name = "district_short_name", nullable = false)
    String shortName;

    @Column(name = "district_type", length = 50, nullable = false)
    String type;

    @Column(name = "district_key")
    String key;

    @Column(name = "district_short_key")
    String shortKey;

    @Column(name = "district_latitude", precision = 10, scale = 7)
    BigDecimal latitude;

    @Column(name = "district_longitude", precision = 10, scale = 7)
    BigDecimal longitude;

    @Column(name = "district_bounds", columnDefinition = "TEXT")
    String bounds;

    @Column(name = "district_geo_address", columnDefinition = "TEXT")
    String geoAddress;

    @Column(name = "district_short_duplicated")
    Boolean shortDuplicated;

    @Column(name = "district_alias", length = 100)
    String alias;

    @Column(name = "district_keywords", columnDefinition = "TEXT")
    String keywords;

    // Province info (denormalized for query optimization)
    @Column(name = "province_name", nullable = false)
    String provinceName;

    @Column(name = "province_short_name", nullable = false)
    String provinceShortName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code", referencedColumnName = "province_code", nullable = false)
    LegacyProvince province;

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<LegacyWard> wards;
}
