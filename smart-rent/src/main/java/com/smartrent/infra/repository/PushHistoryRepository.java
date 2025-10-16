package com.smartrent.infra.repository;

import com.smartrent.enums.PushSource;
import com.smartrent.infra.repository.entity.PushHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PushHistoryRepository extends JpaRepository<PushHistory, Long> {

    List<PushHistory> findByListingId(Long listingId);

    List<PushHistory> findByUserId(String userId);

    List<PushHistory> findByListingIdOrderByPushedAtDesc(Long listingId);

    List<PushHistory> findByUserIdOrderByPushedAtDesc(String userId);

    List<PushHistory> findByPushSource(PushSource pushSource);

    @Query("SELECT ph FROM push_history ph WHERE ph.listingId = :listingId AND ph.pushedAt BETWEEN :startDate AND :endDate ORDER BY ph.pushedAt DESC")
    List<PushHistory> findByListingIdAndDateRange(@Param("listingId") Long listingId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ph FROM push_history ph WHERE ph.userId = :userId AND ph.pushedAt BETWEEN :startDate AND :endDate ORDER BY ph.pushedAt DESC")
    List<PushHistory> findByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(ph) FROM push_history ph WHERE ph.listingId = :listingId")
    Long countByListingId(@Param("listingId") Long listingId);

    @Query("SELECT COUNT(ph) FROM push_history ph WHERE ph.userId = :userId AND ph.pushSource = :source")
    Long countByUserIdAndSource(@Param("userId") String userId, @Param("source") PushSource source);
}

