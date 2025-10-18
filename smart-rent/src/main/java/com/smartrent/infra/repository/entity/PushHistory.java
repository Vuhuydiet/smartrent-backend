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

import java.time.LocalDateTime;

/**
 * Entity representing the history of listing push operations.
 * Records each push attempt with its status and any relevant messages.
 */
@Entity(name = "push_history")
@Table(name = "push_history",
        indexes = {
                @Index(name = "idx_schedule_id", columnList = "schedule_id"),
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_pushed_at", columnList = "pushed_at"),
                @Index(name = "idx_schedule_status", columnList = "schedule_id, status")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushHistory {

    @Id
    @Column(name = "push_history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long pushHistoryId;

    @Column(name = "schedule_id", nullable = false)
    Long scheduleId;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    PushStatus status;

    /**
     * Reason for failure or additional information about the push
     */
    @Column(name = "message", length = 500)
    String message;

    /**
     * Actual time when push was executed
     */
    @Column(name = "pushed_at")
    LocalDateTime pushedAt;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    // Relationships
    @ManyToOne
    @JoinColumn(name = "schedule_id", insertable = false, updatable = false)
    PushSchedule pushSchedule;

    @ManyToOne
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    /**
     * Enum for push status
     */
    public enum PushStatus {
        /**
         * Push was successful
         */
        SUCCESS,

        /**
         * Push failed
         */
        FAIL
    }
}
