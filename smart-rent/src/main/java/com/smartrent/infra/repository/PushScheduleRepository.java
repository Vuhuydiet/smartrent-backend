package com.smartrent.infra.repository;

import com.smartrent.enums.ScheduleStatus;
import com.smartrent.infra.repository.entity.PushSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface PushScheduleRepository extends JpaRepository<PushSchedule, Long> {

    List<PushSchedule> findByUserId(String userId);

    List<PushSchedule> findByListingId(Long listingId);

    List<PushSchedule> findByUserIdAndStatus(String userId, ScheduleStatus status);

    List<PushSchedule> findByListingIdAndStatus(Long listingId, ScheduleStatus status);

    List<PushSchedule> findByUserIdAndListingIdAndStatus(String userId, Long listingId, ScheduleStatus status);

    @Query("SELECT ps FROM push_schedule ps WHERE ps.status = 'ACTIVE' AND ps.scheduledTime = :time")
    List<PushSchedule> findActiveSchedulesByTime(@Param("time") LocalTime time);

    @Query("SELECT ps FROM push_schedule ps WHERE ps.status = 'ACTIVE' AND ps.usedPushes < ps.totalPushes")
    List<PushSchedule> findAllActiveSchedules();

    @Query("SELECT COUNT(ps) FROM push_schedule ps WHERE ps.userId = :userId AND ps.status = 'ACTIVE'")
    Long countActiveSchedulesByUserId(@Param("userId") String userId);
}

