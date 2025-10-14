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

@Entity(name = "provinces")
@Table(name = "provinces",
        indexes = {
                @Index(name = "idx_name", columnList = "name"),
                @Index(name = "idx_is_active", columnList = "is_active"),
                @Index(name = "idx_effective_period", columnList = "effective_from, effective_to"),
                @Index(name = "idx_parent_province", columnList = "parent_province_id"),
                @Index(name = "idx_is_merged", columnList = "is_merged"),
                @Index(name = "idx_structure_version", columnList = "structure_version")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_province_code", columnNames = {"code"})
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long provinceId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 10)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProvinceType type;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "effective_from")
    LocalDate effectiveFrom;

    @Column(name = "effective_to")
    LocalDate effectiveTo;

    // Self-referencing relationship for merged provinces
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_province_id")
    Province parentProvince;

    @OneToMany(mappedBy = "parentProvince", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Province> mergedProvinces;

    @Builder.Default
    @Column(name = "is_merged", nullable = false)
    Boolean isMerged = false;

    @Column(name = "merged_date")
    LocalDate mergedDate;

    @Column(name = "original_name", length = 100)
    String originalName; // Store original name before merger

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_version", nullable = false)
    @Builder.Default
    AdministrativeStructure structureVersion = AdministrativeStructure.BOTH;

    // Relationships with other entities
    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<District> districts;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public enum ProvinceType {
        PROVINCE, CITY
    }

    // Helper methods for merged province logic
    public boolean isParentProvince() {
        return mergedProvinces != null && !mergedProvinces.isEmpty();
    }

    public boolean isMergedProvince() {
        return parentProvince != null;
    }

    public String getDisplayName() {
        return isMergedProvince() ? parentProvince.getName() : name;
    }

    public List<Province> getAllMergedProvinces() {
        return isParentProvince() ? mergedProvinces : List.of();
    }
}