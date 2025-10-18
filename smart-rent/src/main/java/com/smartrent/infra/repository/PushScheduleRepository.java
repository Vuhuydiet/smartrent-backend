package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.PushSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PushSchedule entity.
 * Provides data access methods for managing listing push schedules.
 */
@Repository
public interface PushScheduleRepository extends JpaRepository<PushSchedule, Long> {

    /**
     * Find all active schedules for a specific scheduled time
     *
     * @param scheduledTime The time of day to find schedules for
     * @return List of active schedules
     */
    @Query("SELECT ps FROM push_schedule ps WHERE ps.scheduledTime = :scheduledTime " +
            "AND ps.status = 'ACTIVE'")
    List<PushSchedule> findActiveSchedulesByScheduledTime(
            @Param("scheduledTime") LocalTime scheduledTime
    );

    /**
     * Find active schedule for a specific listing
     *
     * @param listingId The listing ID
     * @return Optional of active schedule
     */
    Optional<PushSchedule> findByListingIdAndStatus(
            Long listingId,
            PushSchedule.ScheduleStatus status
    );

    /**
     * Check if a listing has an active schedule
     *
     * @param listingId The listing ID
     * @return true if active schedule exists
     */
    boolean existsByListingIdAndStatus(
            Long listingId,
            PushSchedule.ScheduleStatus status
    );

    /**
     * Find all schedules by listing ID
     *
     * @param listingId The listing ID
     * @return List of schedules for the listing
     */
    List<PushSchedule> findByListingId(Long listingId);

    /**
     * Find all completed or cancelled schedules
     *
     * @return List of completed or cancelled schedules
     */
    @Query("SELECT ps FROM push_schedule ps WHERE ps.status IN ('COMPLETED', 'CANCELLED')")
    List<PushSchedule> findCompletedOrCancelledSchedules();

    /**
     * Find active schedules that should run at the given time
     *
     * @param scheduledTime The time to match
     * @return List of active schedules matching the time
     */
    @Query("SELECT ps FROM push_schedule ps WHERE ps.scheduledTime = :scheduledTime " +
            "AND ps.status = 'ACTIVE'")
    List<PushSchedule> findActiveSchedulesByTime(@Param("scheduledTime") LocalTime scheduledTime);
}
