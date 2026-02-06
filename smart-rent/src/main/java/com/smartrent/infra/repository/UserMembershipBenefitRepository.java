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

    @Query("SELECT umb FROM user_membership_benefits umb WHERE umb.userId = :userId AND umb.benefitType = :benefitType AND umb.status = 'ACTIVE' AND umb.expiresAt > :now AND umb.quantityUsed < umb.totalQuantity ORDER BY umb.expiresAt ASC")
    List<UserMembershipBenefit> findAvailableBenefits(@Param("userId") String userId, @Param("benefitType") BenefitType benefitType, @Param("now") LocalDateTime now);

    @Query(value = "SELECT * FROM user_membership_benefits umb WHERE umb.user_id = :userId AND umb.benefit_type = :benefitType AND umb.status = 'ACTIVE' AND umb.expires_at > :now AND umb.quantity_used < umb.total_quantity ORDER BY umb.expires_at ASC LIMIT 1", nativeQuery = true)
    Optional<UserMembershipBenefit> findFirstAvailableBenefit(@Param("userId") String userId, @Param("benefitType") String benefitType, @Param("now") LocalDateTime now);

    @Query("SELECT umb FROM user_membership_benefits umb WHERE umb.status = 'ACTIVE' AND umb.expiresAt <= :now")
    List<UserMembershipBenefit> findExpiredBenefits(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE user_membership_benefits umb SET umb.status = 'EXPIRED' WHERE umb.status = 'ACTIVE' AND umb.expiresAt <= :now")
    int expireOldBenefits(@Param("now") LocalDateTime now);

    @Query("SELECT SUM(umb.totalQuantity - umb.quantityUsed) FROM user_membership_benefits umb WHERE umb.userId = :userId AND umb.benefitType = :benefitType AND umb.status = 'ACTIVE' AND umb.expiresAt > :now")
    Integer getTotalAvailableQuota(@Param("userId") String userId, @Param("benefitType") BenefitType benefitType, @Param("now") LocalDateTime now);

    @Query("SELECT SUM(umb.totalQuantity) FROM user_membership_benefits umb WHERE umb.userId = :userId AND umb.benefitType = :benefitType AND umb.status = 'ACTIVE' AND umb.expiresAt > :now")
    Integer getTotalGrantedQuota(@Param("userId") String userId, @Param("benefitType") BenefitType benefitType, @Param("now") LocalDateTime now);

    @Query("SELECT SUM(umb.quantityUsed) FROM user_membership_benefits umb WHERE umb.userId = :userId AND umb.benefitType = :benefitType AND umb.status = 'ACTIVE' AND umb.expiresAt > :now")
    Integer getTotalUsedQuota(@Param("userId") String userId, @Param("benefitType") BenefitType benefitType, @Param("now") LocalDateTime now);

    List<UserMembershipBenefit> findByUserMembershipUserMembershipId(Long userMembershipId);
}

