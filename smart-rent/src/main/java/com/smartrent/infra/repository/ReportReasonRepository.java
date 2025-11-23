package com.smartrent.infra.repository;

import com.smartrent.enums.ReportCategory;
import com.smartrent.infra.repository.entity.ReportReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportReasonRepository extends JpaRepository<ReportReason, Long> {

    /**
     * Find all active report reasons
     */
    List<ReportReason> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find active report reasons by category
     */
    List<ReportReason> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(ReportCategory category);
}

