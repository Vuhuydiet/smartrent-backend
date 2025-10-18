package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.PushDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushDetailRepository extends JpaRepository<PushDetail, Long> {

    /**
     * Find push detail by detail code
     */
    Optional<PushDetail> findByDetailCode(String detailCode);

    /**
     * Find all active push details ordered by display order
     */
    List<PushDetail> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find all push details ordered by display order
     */
    List<PushDetail> findAllByOrderByDisplayOrderAsc();

    /**
     * Check if detail code exists
     */
    boolean existsByDetailCode(String detailCode);
}

