package com.smartrent.infra.repository.entity;

import com.smartrent.enums.ReportCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Predefined report reasons that users can select when reporting a listing
 */
@Entity(name = "report_reasons")
@Table(name = "report_reasons",
        indexes = {
                @Index(name = "idx_category", columnList = "category"),
                @Index(name = "idx_is_active", columnList = "is_active")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reason_id")
    Long reasonId;

    @Column(name = "reason_text", nullable = false, length = 500)
    String reasonText;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    ReportCategory category;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "display_order")
    Integer displayOrder;

    @ManyToMany(mappedBy = "reportReasons")
    List<ListingReport> listingReports;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}

