package com.smartrent.infra.repository.entity;

import com.smartrent.enums.ReportCategory;
import com.smartrent.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a report submitted by a user about a listing
 * Users can report issues with listing information or map-related problems
 */
@Entity(name = "listing_reports")
@Table(name = "listing_reports",
        indexes = {
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_reporter_email", columnList = "reporter_email"),
                @Index(name = "idx_reporter_phone", columnList = "reporter_phone"),
                @Index(name = "idx_category", columnList = "category"),
                @Index(name = "idx_created_at", columnList = "created_at"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_resolved_by", columnList = "resolved_by")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    Long reportId;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    @Column(name = "reporter_name", length = 200)
    String reporterName;

    @Column(name = "reporter_phone", nullable = false, length = 20)
    String reporterPhone;

    @Column(name = "reporter_email", nullable = false, length = 255)
    String reporterEmail;

    @ManyToMany
    @JoinTable(
            name = "listing_report_reasons",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "reason_id")
    )
    List<ReportReason> reportReasons;

    @Column(name = "other_feedback", columnDefinition = "TEXT")
    String otherFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    ReportCategory category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    ReportStatus status = ReportStatus.PENDING;

    @Column(name = "resolved_by", length = 36)
    String resolvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by", insertable = false, updatable = false)
    Admin admin;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}

