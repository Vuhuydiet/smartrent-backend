package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mapping between legacy province (integer ID) and new province (code)
 */
@Entity
@Table(name = "province_mapping",
        indexes = {
                @Index(name = "idx_province_mapping_legacy_id", columnList = "province_legacy_id"),
                @Index(name = "idx_province_mapping_new_code", columnList = "province_new_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_province_mapping", columnNames = {"province_legacy_id", "province_new_code"})
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProvinceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_legacy_id", nullable = false)
    LegacyProvince legacyProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_new_code", nullable = false)
    Province newProvince;

    @Column(name = "effective_date")
    LocalDate effectiveDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}