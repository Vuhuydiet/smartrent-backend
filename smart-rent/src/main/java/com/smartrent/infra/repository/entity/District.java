package com.smartrent.infra.repository.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "districts")
@Table(name = "districts",
        indexes = {
                @Index(name = "idx_province_id", columnList = "province_id"),
                @Index(name = "idx_name", columnList = "name"),
                @Index(name = "idx_is_active", columnList = "is_active"),
                @Index(name = "idx_effective_period", columnList = "effective_from, effective_to"),
                @Index(name = "idx_structure_version", columnList = "structure_version")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_province_district_code", columnNames = {"province_id", "code"})
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
    Long districtId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 10)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DistrictType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    Province province;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "effective_from")
    LocalDate effectiveFrom;

    @Column(name = "effective_to")
    LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_version", nullable = false)
    @Builder.Default
    AdministrativeStructure structureVersion = AdministrativeStructure.OLD;

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Ward> wards;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public enum DistrictType {
        DISTRICT, TOWN, CITY
    }
}