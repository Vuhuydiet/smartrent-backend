package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mapping between legacy ward and new ward
 * Tracks merge types: unchanged, merged_with_others, split_to_multiple, renamed
 */
@Entity
@Table(name = "ward_mapping",
        indexes = {
                @Index(name = "idx_ward_mapping_legacy_id", columnList = "ward_legacy_id"),
                @Index(name = "idx_ward_mapping_new_code", columnList = "ward_new_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ward_mapping", columnNames = {"ward_legacy_id", "ward_new_code"})
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WardMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_legacy_id", nullable = false)
    LegacyWard legacyWard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_new_code", nullable = false)
    Ward newWard;

    @Enumerated(EnumType.STRING)
    @Column(name = "merge_type", length = 50)
    MergeType mergeType;

    @Column(name = "effective_date")
    LocalDate effectiveDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    /**
     * Indicates if this is the default/primary mapping for the new ward
     * When multiple legacy wards merge into one new ward, only one should be default (1)
     * When one legacy ward splits to multiple new wards, only one should be default (1)
     */
    @Column(name = "is_default_new_ward")
    Boolean isDefaultNewWard;

    public enum MergeType {
        UNCHANGED,           // Ward unchanged (was 'exact' in migrations)
        MERGED_WITH_OTHERS,  // Multiple wards merged (was 'partial' in migrations)
        SPLIT_TO_MULTIPLE,   // Ward split into multiple (was 'split_to_multiple' in migrations)
        RENAMED              // Ward renamed (was 'exact' in migrations)
    }
}