package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mapping between legacy district and new ward
 * Districts were eliminated in the new structure
 */
@Entity
@Table(name = "district_ward_mapping",
        indexes = {
                @Index(name = "idx_district_ward_mapping_district_id", columnList = "district_legacy_id"),
                @Index(name = "idx_district_ward_mapping_ward_code", columnList = "ward_new_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_district_ward_mapping", columnNames = {"district_legacy_id", "ward_new_code"})
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DistrictWardMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_legacy_id", nullable = false)
    District legacyDistrict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_new_code", nullable = false)
    Ward newWard;

    @Column(name = "effective_date")
    LocalDate effectiveDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}