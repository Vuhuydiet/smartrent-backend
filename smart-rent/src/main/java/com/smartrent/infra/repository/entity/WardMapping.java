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

    public enum MergeType {
        UNCHANGED,
        MERGED_WITH_OTHERS,
        SPLIT_TO_MULTIPLE,
        RENAMED
    }
}