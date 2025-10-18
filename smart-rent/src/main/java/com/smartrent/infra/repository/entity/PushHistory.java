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
                @Index(name = "idx_listing_id", columnList = "listing_id"),
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
    @Column(name = "push_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long pushId;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_source", nullable = false)
    PushSource pushSource;

    @Column(name = "user_benefit_id")
    Long userBenefitId;

    @Column(name = "schedule_id")
    Long scheduleId;

    @Column(name = "transaction_id", length = 36)
    String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    PushStatus status;

    @Column(name = "message", length = 500)
    String message;

    @Column(name = "pushed_at")
    @CreationTimestamp
    LocalDateTime pushedAt;

    // Relationships
    @ManyToOne
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    @ManyToOne
    @JoinColumn(name = "user_benefit_id", insertable = false, updatable = false)
    UserMembershipBenefit userMembershipBenefit;

    @ManyToOne
    @JoinColumn(name = "schedule_id", insertable = false, updatable = false)
    PushSchedule schedule;

    @ManyToOne
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    Transaction transaction;

    /**
     * Enum for push source
     */
    public enum PushSource {
        /**
         * Push from membership quota
         */
        MEMBERSHIP_QUOTA,

        /**
         * Direct purchase/payment
         */
        DIRECT_PURCHASE,

        /**
         * Direct payment (after completing payment)
         */
        DIRECT_PAYMENT,

        /**
         * Scheduled push
         */
        SCHEDULED,

        /**
         * Admin push
         */
        ADMIN
    }

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
