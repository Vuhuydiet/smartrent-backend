package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mapping between legacy street and new province/ward structure
 */
@Entity
@Table(name = "street_mapping",
        indexes = {
                @Index(name = "idx_street_mapping_legacy_id", columnList = "street_legacy_id"),
                @Index(name = "idx_street_mapping_province_code", columnList = "province_new_code"),
                @Index(name = "idx_street_mapping_ward_code", columnList = "ward_new_code")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StreetMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "street_legacy_id", nullable = false)
    Street legacyStreet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_new_code", nullable = false)
    Province newProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_new_code")
    Ward newWard;

    @Column(name = "effective_date")
    LocalDate effectiveDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}