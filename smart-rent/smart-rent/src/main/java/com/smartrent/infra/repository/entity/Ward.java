package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * New ward structure (effective from July 1, 2025)
 * Directly linked to province (no district level)
 * Maps to wards table from V22 migration
 * Contains denormalized province information for query optimization
 */
@Entity
@Table(name = "wards",
        indexes = {
                @Index(name = "idx_wards_code", columnList = "ward_code"),
                @Index(name = "idx_wards_province_code", columnList = "province_code"),
                @Index(name = "idx_wards_key", columnList = "ward_key"),
                @Index(name = "idx_wards_name", columnList = "ward_name")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ward_id")
    Integer id;

    @Column(name = "province_code", length = 10, nullable = false, insertable = false, updatable = false)
    String provinceCode;

    @Column(name = "ward_code", length = 10, nullable = false, unique = true)
    String code;

    // Denormalized province information
    @Column(name = "province_name", nullable = false)
    String provinceName;

    @Column(name = "province_short_name", nullable = false)
    String provinceShortName;

    @Column(name = "ward_name", nullable = false)
    String name;

    @Column(name = "ward_short_name", nullable = false)
    String shortName;

    @Column(name = "ward_type", length = 50, nullable = false)
    String type;

    @Column(name = "province_key")
    String provinceKey;

    @Column(name = "province_short_key")
    String provinceShortKey;

    @Column(name = "ward_key")
    String key;

    @Column(name = "ward_short_key")
    String shortKey;

    @Column(name = "province_latitude", precision = 10, scale = 7)
    BigDecimal provinceLatitude;

    @Column(name = "province_longitude", precision = 10, scale = 7)
    BigDecimal provinceLongitude;

    @Column(name = "ward_latitude", precision = 10, scale = 7)
    BigDecimal latitude;

    @Column(name = "ward_longitude", precision = 10, scale = 7)
    BigDecimal longitude;

    @Column(name = "ward_area_km2", precision = 10, scale = 2)
    BigDecimal areaKm2;

    @Column(name = "ward_key_duplicated")
    Boolean keyDuplicated;

    @Column(name = "ward_short_key_duplicated")
    Boolean shortKeyDuplicated;

    @Column(name = "ward_unique")
    Boolean unique;

    @Column(name = "ward_alias", length = 100)
    String alias;

    @Column(name = "ward_keywords", columnDefinition = "TEXT")
    String keywords;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationship - references province by code
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code", referencedColumnName = "province_code", nullable = false)
    Province province;
}