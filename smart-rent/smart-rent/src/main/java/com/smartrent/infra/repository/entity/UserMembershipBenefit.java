package com.smartrent.infra.repository.entity;

import com.smartrent.enums.BenefitStatus;
import com.smartrent.enums.BenefitType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "user_membership_benefits")
@Table(name = "user_membership_benefits",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_benefit_type", columnList = "benefit_type"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_user_benefit_status", columnList = "user_id, benefit_type, status"),
                @Index(name = "idx_expires_at", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_membership_benefit", columnNames = {"user_membership_id", "benefit_id"})
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserMembershipBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_benefit_id")
    Long userBenefitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_membership_id", nullable = false)
    UserMembership userMembership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_id", nullable = false)
    MembershipPackageBenefit packageBenefit;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false)
    BenefitType benefitType;

    @Column(name = "granted_at", nullable = false)
    LocalDateTime grantedAt;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "total_quantity", nullable = false)
    Integer totalQuantity;

    @Builder.Default
    @Column(name = "quantity_used", nullable = false)
    Integer quantityUsed = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    BenefitStatus status = BenefitStatus.ACTIVE;

    @OneToMany(mappedBy = "userMembershipBenefit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<PushHistory> pushHistories;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Helper methods
    public Integer getQuantityRemaining() {
        return totalQuantity - quantityUsed;
    }

    public boolean hasQuotaAvailable() {
        return status == BenefitStatus.ACTIVE &&
               quantityUsed < totalQuantity &&
               LocalDateTime.now().isBefore(expiresAt);
    }

    public boolean isFullyUsed() {
        return quantityUsed >= totalQuantity;
    }

    public boolean isExpired() {
        return status == BenefitStatus.EXPIRED || LocalDateTime.now().isAfter(expiresAt);
    }

    public void consumeQuota(int quantity) {
        if (!hasQuotaAvailable()) {
            throw new IllegalStateException("No quota available for benefit: " + benefitType);
        }
        if (quantityUsed + quantity > totalQuantity) {
            throw new IllegalArgumentException("Insufficient quota. Available: " + getQuantityRemaining() + ", Requested: " + quantity);
        }
        this.quantityUsed += quantity;
        if (isFullyUsed()) {
            this.status = BenefitStatus.FULLY_USED;
        }
    }

    public void expire() {
        this.status = BenefitStatus.EXPIRED;
    }
}

