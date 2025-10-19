package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
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

@Entity(name = "wards")
@Table(name = "wards",
        indexes = {
                @Index(name = "idx_district_id", columnList = "district_id"),
                @Index(name = "idx_name", columnList = "name"),
                @Index(name = "idx_is_active", columnList = "is_active"),
                @Index(name = "idx_effective_period", columnList = "effective_from, effective_to")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_district_ward_code", columnNames = {"district_id", "code"})
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
    Long wardId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 10)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WardType type;

    // District relationship - nullable to support 2025 structure (Province -> Ward directly)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = true)
    District district;

    // Direct province relationship for 2025 structure (no districts)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = true)
    Province province;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_2025_structure", nullable = false)
    Boolean is2025Structure = false; // true if belongs to new province-only structure

    @Column(name = "effective_from")
    LocalDate effectiveFrom;

    @Column(name = "effective_to")
    LocalDate effectiveTo;

    @OneToMany(mappedBy = "ward", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Street> streets;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public enum WardType {
        WARD, COMMUNE, TOWNSHIP
    }

    // Helper methods for 2025 structure
    public Province getEffectiveProvince() {
        if (is2025Structure != null && is2025Structure) {
            return province;
        }
        return district != null ? district.getProvince() : null;
    }

    public boolean hasDistrict() {
        return district != null;
    }

    public boolean usesNewStructure() {
        return is2025Structure != null && is2025Structure;
    }
}