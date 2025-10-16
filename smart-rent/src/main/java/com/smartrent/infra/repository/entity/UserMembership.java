package com.smartrent.infra.repository.entity;

import com.smartrent.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "user_memberships")
@Table(name = "user_memberships",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_end_date", columnList = "end_date"),
                @Index(name = "idx_user_status", columnList = "user_id, status")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_membership_id")
    Long userMembershipId;

    @Column(name = "user_id", nullable = false)
    String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    MembershipPackage membershipPackage;

    @Column(name = "start_date", nullable = false)
    LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    LocalDateTime endDate;

    @Column(name = "duration_days", nullable = false)
    Integer durationDays;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "total_paid", nullable = false, precision = 15, scale = 0)
    BigDecimal totalPaid;

    @OneToMany(mappedBy = "userMembership", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<UserMembershipBenefit> benefits;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Helper methods
    public boolean isActive() {
        return status == MembershipStatus.ACTIVE && LocalDateTime.now().isBefore(endDate);
    }

    public boolean isExpired() {
        return status == MembershipStatus.EXPIRED || LocalDateTime.now().isAfter(endDate);
    }

    public boolean isCancelled() {
        return status == MembershipStatus.CANCELLED;
    }

    public long getDaysRemaining() {
        if (isExpired() || isCancelled()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), endDate).toDays();
    }

    public void expire() {
        this.status = MembershipStatus.EXPIRED;
    }

    public void cancel() {
        this.status = MembershipStatus.CANCELLED;
    }
}

