package com.smartrent.infra.repository.entity;

import com.smartrent.enums.ScheduleSource;
import com.smartrent.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity(name = "push_schedule")
@Table(name = "push_schedule",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_scheduled_time", columnList = "scheduled_time"),
                @Index(name = "idx_user_listing_status", columnList = "user_id, listing_id, status")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    Long scheduleId;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    @Column(name = "scheduled_time", nullable = false)
    LocalTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    ScheduleSource source;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    Transaction transaction;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<PushHistory> pushHistories;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Helper methods
    public Integer getPushesRemaining() {
        return totalPushes - usedPushes;
    }

    public boolean hasRemainingPushes() {
        return status == ScheduleStatus.ACTIVE && usedPushes < totalPushes;
    }

    public boolean isCompleted() {
        return status == ScheduleStatus.COMPLETED || usedPushes >= totalPushes;
    }

    public boolean isCancelled() {
        return status == ScheduleStatus.CANCELLED;
    }

    public void incrementUsedPushes() {
        this.usedPushes++;
        if (usedPushes >= totalPushes) {
            this.status = ScheduleStatus.COMPLETED;
        }
    }

    public void cancel() {
        this.status = ScheduleStatus.CANCELLED;
    }

    public boolean isFromMembership() {
        return source == ScheduleSource.MEMBERSHIP;
    }

    public boolean isFromDirectPurchase() {
        return source == ScheduleSource.DIRECT_PURCHASE;
    }
}

