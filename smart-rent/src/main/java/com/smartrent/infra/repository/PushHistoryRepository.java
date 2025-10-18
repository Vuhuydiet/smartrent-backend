package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.PushHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for PushHistory entity.
 * Provides data access methods for managing listing push history records.
 */
@Repository
public interface PushHistoryRepository extends JpaRepository<PushHistory, Long> {

    /**
     * Find all push history records for a specific schedule
     *
     * @param scheduleId The schedule ID
     * @return List of push history records
     */
    List<PushHistory> findByScheduleId(Long scheduleId);

    /**
     * Find all push history records for a specific listing
     *
     * @param listingId The listing ID
     * @return List of push history records
     */
    List<PushHistory> findByListingId(Long listingId);

    /**
     * Find push history records by status
     *
     * @param status The push status
     * @return List of push history records
     */
    List<PushHistory> findByStatus(PushHistory.PushStatus status);

    /**
     * Find push history records within a date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of push history records
     */
    @Query("SELECT ph FROM push_history ph WHERE ph.pushedAt BETWEEN :startDate AND :endDate")
    List<PushHistory> findByPushedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find recent push history for a specific listing
     *
     * @param listingId The listing ID
     * @param limit Number of records to return
     * @return List of recent push history records
     */
    @Query("SELECT ph FROM push_history ph WHERE ph.listingId = :listingId " +
            "ORDER BY ph.pushedAt DESC LIMIT :limit")
    List<PushHistory> findRecentByListingId(
            @Param("listingId") Long listingId,
            @Param("limit") int limit
    );

    /**
     * Count successful pushes for a schedule
     *
     * @param scheduleId The schedule ID
     * @return Count of successful pushes
     */
    @Query("SELECT COUNT(ph) FROM push_history ph WHERE ph.scheduleId = :scheduleId " +
            "AND ph.status = 'SUCCESS'")
    Long countSuccessfulPushesByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * Count failed pushes for a schedule
     *
     * @param scheduleId The schedule ID
     * @return Count of failed pushes
     */
    @Query("SELECT COUNT(ph) FROM push_history ph WHERE ph.scheduleId = :scheduleId " +
            "AND ph.status = 'FAIL'")
    Long countFailedPushesByScheduleId(@Param("scheduleId") Long scheduleId);
}
