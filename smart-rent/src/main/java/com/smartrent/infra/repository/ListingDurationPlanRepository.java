package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.ListingDurationPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingDurationPlanRepository extends JpaRepository<ListingDurationPlan, Long> {
    
    /**
     * Find all active plans ordered by duration days ascending
     */
    List<ListingDurationPlan> findAllByIsActiveTrueOrderByDurationDaysAsc();
    
    /**
     * Find plan by duration days
     */
    Optional<ListingDurationPlan> findByDurationDays(Integer durationDays);
    
    /**
     * Check if plan exists and is active
     */
    boolean existsByPlanIdAndIsActiveTrue(Long planId);
}
