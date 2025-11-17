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
 * Legacy province structure (63 provinces before July 1, 2025)
 * Maps to legacy_provinces table from V32 migration
 */
@Entity
@Table(name = "legacy_provinces",
        indexes = {
                @Index(name = "idx_legacy_provinces_code", columnList = "province_code"),
                @Index(name = "idx_legacy_provinces_key", columnList = "province_key")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyProvince {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_province_id")
    Integer id;

    @Column(name = "province_code", length = 10, nullable = false, unique = true)
    String code;

    @Column(name = "province_name", nullable = false)
    String name;

    @Column(name = "province_short_name", nullable = false)
    String shortName;

    @Column(name = "province_key", nullable = false)
    String key;

    @Column(name = "province_short_key")
    String shortKey;

    @Column(name = "province_latitude", precision = 10, scale = 7)
    BigDecimal latitude;

    @Column(name = "province_longitude", precision = 10, scale = 7)
    BigDecimal longitude;

    @Column(name = "province_bounds", columnDefinition = "TEXT")
    String bounds;

    @Column(name = "province_geo_address", columnDefinition = "TEXT")
    String geoAddress;

    @Column(name = "province_alias", length = 50)
    String alias;

    @Column(name = "province_keywords", columnDefinition = "TEXT")
    String keywords;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationships - districts reference this province by province_code
    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<District> districts;
}
