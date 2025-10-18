package com.smartrent.infra.repository.entity;

import com.smartrent.enums.PushSource;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity(name = "push_history")
@Table(name = "push_history",
        indexes = {
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_pushed_at", columnList = "pushed_at"),
                @Index(name = "idx_listing_pushed", columnList = "listing_id, pushed_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_id")
    Long pushId;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_source", nullable = false)
    PushSource pushSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_benefit_id")
    UserMembershipBenefit userBenefit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    PushSchedule schedule;

    @Column(name = "transaction_id", length = 36)
    String transactionId;

    @CreationTimestamp
    @Column(name = "pushed_at")
    LocalDateTime pushedAt;

    // Helper methods
    public boolean isFromMembershipQuota() {
        return pushSource == PushSource.MEMBERSHIP_QUOTA;
    }

    public boolean isFromDirectPayment() {
        return pushSource == PushSource.DIRECT_PAYMENT;
    }

    public boolean isScheduled() {
        return pushSource == PushSource.SCHEDULED;
    }

    public boolean isAdminPush() {
        return pushSource == PushSource.ADMIN;
    }

    public boolean hasLinkedTransaction() {
        return transactionId != null && !transactionId.isEmpty();
    }
}

