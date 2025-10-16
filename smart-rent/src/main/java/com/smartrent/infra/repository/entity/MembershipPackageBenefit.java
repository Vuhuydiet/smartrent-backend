package com.smartrent.infra.repository.entity;

import com.smartrent.enums.BenefitType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "membership_package_benefits")
@Table(name = "membership_package_benefits",
        indexes = {
                @Index(name = "idx_membership_id", columnList = "membership_id"),
                @Index(name = "idx_benefit_type", columnList = "benefit_type")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MembershipPackageBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "benefit_id")
    Long benefitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    MembershipPackage membershipPackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false)
    BenefitType benefitType;

    @Column(name = "benefit_name_display", nullable = false, length = 200)
    String benefitNameDisplay;

    @Builder.Default
    @Column(name = "quantity_per_month", nullable = false)
    Integer quantityPerMonth = 0;

    @OneToMany(mappedBy = "packageBenefit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<UserMembershipBenefit> userBenefits;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    // Helper methods
    public Integer calculateTotalQuantity(Integer durationMonths) {
        return quantityPerMonth * durationMonths;
    }

    public boolean isSilverPostBenefit() {
        return benefitType == BenefitType.POST_SILVER;
    }

    public boolean isGoldPostBenefit() {
        return benefitType == BenefitType.POST_GOLD;
    }

    public boolean isDiamondPostBenefit() {
        return benefitType == BenefitType.POST_DIAMOND;
    }

    public boolean isBoostBenefit() {
        return benefitType == BenefitType.BOOST;
    }

    public boolean isAutoApproveBenefit() {
        return benefitType == BenefitType.AUTO_APPROVE;
    }

    public boolean isBadgeBenefit() {
        return benefitType == BenefitType.BADGE;
    }
}

