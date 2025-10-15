package com.smartrent.infra.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
 * Each listing can have at most one ACTIVE schedule at a time.
 * The scheduled_time field stores the hour of the day when the listing should be pushed.
 */
@Entity(name = "push_schedules")
@Table(name = "push_schedules",
        indexes = {
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_scheduled_time", columnList = "scheduled_time"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_listing_status", columnList = "listing_id, status"),
                @Index(name = "idx_end_time", columnList = "end_time")
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

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    /**
     * Hour of the day to push (e.g., 09:00:00, 15:00:00)
     * The push will occur at the start of this hour
     */
    @Column(name = "scheduled_time", nullable = false)
    LocalTime scheduledTime;

    /**
     * After this time, the schedule will no longer be processed
     */
    @Column(name = "end_time", nullable = false)
    LocalDateTime endTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    ScheduleStatus status = ScheduleStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    // Relationship to Listing
    @ManyToOne
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    /**
     * Enum for schedule status
     */
    public enum ScheduleStatus {
        /**
         * Schedule is active and will be processed
         */
        ACTIVE,

        /**
         * Schedule is temporarily inactive
         */
        INACTIVE,

        /**
         * Schedule has expired (end_time has passed)
         */
        EXPIRED
    }
}
