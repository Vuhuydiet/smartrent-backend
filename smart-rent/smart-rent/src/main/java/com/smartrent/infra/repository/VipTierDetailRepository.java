package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.VipTierDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VipTierDetailRepository extends JpaRepository<VipTierDetail, Long> {

    /**
     * Find VIP tier by tier code (NORMAL, SILVER, GOLD, DIAMOND)
     */
    Optional<VipTierDetail> findByTierCode(String tierCode);

    /**
     * Find all active VIP tiers ordered by tier level
     */
    List<VipTierDetail> findByIsActiveTrueOrderByTierLevelAsc();

    /**
     * Find all VIP tiers ordered by display order
     */
    List<VipTierDetail> findAllByOrderByDisplayOrderAsc();

    /**
     * Find VIP tier by tier level
     */
    Optional<VipTierDetail> findByTierLevel(Integer tierLevel);

    /**
     * Check if tier code exists
     */
    boolean existsByTierCode(String tierCode);
}

