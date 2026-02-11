package com.smartrent.infra.repository;

import com.smartrent.enums.BenefitType;
import com.smartrent.infra.repository.entity.MembershipPackageBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipPackageBenefitRepository extends JpaRepository<MembershipPackageBenefit, Long> {

    List<MembershipPackageBenefit> findByMembershipPackageMembershipId(Long membershipId);

    List<MembershipPackageBenefit> findByBenefitType(BenefitType benefitType);

    List<MembershipPackageBenefit> findByMembershipPackageMembershipIdAndBenefitType(Long membershipId, BenefitType benefitType);
}

