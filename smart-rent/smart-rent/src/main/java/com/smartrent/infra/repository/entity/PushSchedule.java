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

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing a push schedule for listings.
 * Manages scheduled automatic pushes for listings.
 */
@Entity(name = "push_schedule")
@Table(name = "push_schedule",
        indexes = {
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_scheduled_time", columnList = "scheduled_time")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushSchedule {

    @Id
    @Column(name = "schedule_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long scheduleId;

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    /**
     * Time of day to push the listing (e.g., 09:00:00, 15:00:00)
     */
    @Column(name = "scheduled_time", nullable = false)
    LocalTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    PushSource source;

    /**
     * Reference to the source that created this schedule
     * Could be user_membership_id for MEMBERSHIP source
     */
    @Column(name = "source_id")
    Long sourceId;

    @Builder.Default
    @Column(name = "total_pushes", nullable = false)
    Integer totalPushes = 1;

    @Builder.Default
    @Column(name = "used_pushes", nullable = false)
    Integer usedPushes = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    ScheduleStatus status = ScheduleStatus.ACTIVE;

    @Column(name = "transaction_id", length = 36)
    String transactionId;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    Transaction transaction;

    /**
     * Enum for push source
     */
    public enum PushSource {
        MEMBERSHIP,
        DIRECT_PURCHASE
    }

    /**
     * Enum for schedule status
     */
    public enum ScheduleStatus {
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    // Helper methods
    public boolean isActive() {
        return status == ScheduleStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return status == ScheduleStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == ScheduleStatus.CANCELLED;
    }

    public boolean hasRemainingPushes() {
        return usedPushes < totalPushes;
    }

    public void incrementUsedPushes() {
        this.usedPushes++;
        if (this.usedPushes >= this.totalPushes) {
            this.status = ScheduleStatus.COMPLETED;
        }
    }

    public void cancel() {
        this.status = ScheduleStatus.CANCELLED;
    }
}
