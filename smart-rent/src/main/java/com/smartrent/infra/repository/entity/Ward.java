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
                @Index(name = "idx_effective_period", columnList = "effective_from, effective_to"),
                @Index(name = "idx_parent_ward", columnList = "parent_ward_id"),
                @Index(name = "idx_is_merged", columnList = "is_merged"),
                @Index(name = "idx_structure_version", columnList = "structure_version")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    District district;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "effective_from")
    LocalDate effectiveFrom;

    @Column(name = "effective_to")
    LocalDate effectiveTo;

    // Self-referencing relationship for merged wards
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_ward_id")
    Ward parentWard;

    @OneToMany(mappedBy = "parentWard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Ward> mergedWards;

    @Builder.Default
    @Column(name = "is_merged", nullable = false)
    Boolean isMerged = false;

    @Column(name = "merged_date")
    LocalDate mergedDate;

    @Column(name = "original_name", length = 100)
    String originalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_version", nullable = false)
    @Builder.Default
    AdministrativeStructure structureVersion = AdministrativeStructure.BOTH;

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

    // Helper methods for merged ward logic
    public boolean isParentWard() {
        return mergedWards != null && !mergedWards.isEmpty();
    }

    public boolean isMergedWard() {
        return parentWard != null;
    }

    public String getDisplayName() {
        return isMergedWard() ? parentWard.getName() : name;
    }

    public List<Ward> getAllMergedWards() {
        return isParentWard() ? mergedWards : List.of();
    }
}