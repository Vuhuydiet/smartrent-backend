package com.smartrent.infra.repository;

import com.smartrent.enums.BenefitStatus;
import com.smartrent.enums.BenefitType;
import com.smartrent.infra.repository.entity.UserMembershipBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMembershipBenefitRepository extends JpaRepository<UserMembershipBenefit, Long> {

    List<UserMembershipBenefit> findByUserId(String userId);

    List<UserMembershipBenefit> findByUserIdAndBenefitType(String userId, BenefitType benefitType);

    List<UserMembershipBenefit> findByUserIdAndStatus(String userId, BenefitStatus status);

    List<UserMembershipBenefit> findByUserIdAndBenefitTypeAndStatus(String userId, BenefitType benefitType, BenefitStatus status);

    // Scoped to the user's CURRENT membership slot (same predicate as
    // UserMembershipRepository.findActiveUserMembership) — mapToUserMembershipResponse
    // only ever renders benefits for that one slot, so consumption/availability
    // must agree with it. Without this join, a benefit row that belongs to a
    // queued/upgraded/renewed slot (never shown on the seller's benefit card)
    // could still be picked up and silently decremented here, making a push
    // look like it "used quota" while the visible counter never moves.
    @Query("SELECT umb FROM user_membership_benefits umb JOIN umb.userMembership um " +
            "WHERE umb.userId = :userId AND umb.benefitType = :benefitType AND umb.status = 'ACTIVE' " +
            "AND umb.expiresAt > :now AND umb.quantityUsed < umb.totalQuantity " +
            "AND um.status = 'ACTIVE' AND um.startDate <= :now AND um.endDate > :now " +
            "ORDER BY umb.expiresAt ASC")
    List<UserMembershipBenefit> findAvailableBenefits(@Param("userId") String userId, @Param("benefitType") BenefitType benefitType, @Param("now") LocalDateTime now);

    @Query(value = "SELECT umb.* FROM user_membership_benefits umb " +
            "JOIN user_memberships um ON um.user_membership_id = umb.user_membership_id " +
            "WHERE umb.user_id = :userId AND umb.benefit_type = :benefitType AND umb.status = 'ACTIVE' " +
            "AND umb.expires_at > :now AND umb.quantity_used < umb.total_quantity " +
            "AND um.status = 'ACTIVE' AND um.start_date <= :now AND um.end_date > :now " +
            "ORDER BY umb.expires_at ASC LIMIT 1", nativeQuery = true)
    Optional<UserMembershipBenefit> findFirstAvailableBenefit(@Param("userId") String userId, @Param("benefitType") String benefitType, @Param("now") LocalDateTime now);

    @Query("SELECT umb FROM user_membership_benefits umb WHERE umb.status = 'ACTIVE' AND umb.expiresAt <= :now")
    List<UserMembershipBenefit> findExpiredBenefits(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE user_membership_benefits umb SET umb.status = 'EXPIRED' WHERE umb.status = 'ACTIVE' AND umb.expiresAt <= :now")
    int expireOldBenefits(@Param("now") LocalDateTime now);

    @Query("SELECT SUM(umb.totalQuantity - umb.quantityUsed) FROM user_membership_benefits umb JOIN umb.userMembership um " +
            "WHERE umb.userId = :userId AND umb.benefitType = :benefitType AND umb.status = 'ACTIVE' AND umb.expiresAt > :now " +
            "AND um.status = 'ACTIVE' AND um.startDate <= :now AND um.endDate > :now")
    Integer getTotalAvailableQuota(@Param("userId") String userId, @Param("benefitType") BenefitType benefitType, @Param("now") LocalDateTime now);

    @Query("SELECT SUM(umb.totalQuantity) FROM user_membership_benefits umb JOIN umb.userMembership um " +
            "WHERE umb.userId = :userId AND umb.benefitType = :benefitType AND umb.status = 'ACTIVE' AND umb.expiresAt > :now " +
            "AND um.status = 'ACTIVE' AND um.startDate <= :now AND um.endDate > :now")
    Integer getTotalGrantedQuota(@Param("userId") String userId, @Param("benefitType") BenefitType benefitType, @Param("now") LocalDateTime now);

    @Query("SELECT SUM(umb.quantityUsed) FROM user_membership_benefits umb JOIN umb.userMembership um " +
            "WHERE umb.userId = :userId AND umb.benefitType = :benefitType AND umb.status = 'ACTIVE' AND umb.expiresAt > :now " +
            "AND um.status = 'ACTIVE' AND um.startDate <= :now AND um.endDate > :now")
    Integer getTotalUsedQuota(@Param("userId") String userId, @Param("benefitType") BenefitType benefitType, @Param("now") LocalDateTime now);

    List<UserMembershipBenefit> findByUserMembershipUserMembershipId(Long userMembershipId);
}

