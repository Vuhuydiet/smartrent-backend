package com.smartrent.infra.repository;

import com.smartrent.enums.MembershipStatus;
import com.smartrent.infra.repository.entity.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {

    List<UserMembership> findByUserId(String userId);

    List<UserMembership> findByUserIdAndStatus(String userId, MembershipStatus status);

    // Returns the membership the user is actively using right now (startDate in the past, not yet expired).
    @Query("SELECT um FROM user_memberships um WHERE um.userId = :userId AND um.status = 'ACTIVE' AND um.startDate <= :now AND um.endDate > :now ORDER BY um.endDate DESC")
    Optional<UserMembership> findActiveUserMembership(@Param("userId") String userId, @Param("now") LocalDateTime now);

    // Returns the next queued membership (startDate in the future, still ACTIVE status).
    @Query("SELECT um FROM user_memberships um WHERE um.userId = :userId AND um.status = 'ACTIVE' AND um.startDate > :now ORDER BY um.startDate ASC")
    Optional<UserMembership> findQueuedMembership(@Param("userId") String userId, @Param("now") LocalDateTime now);

    // Returns current (non-queued) ACTIVE memberships whose endDate has passed — used by the lifecycle job.
    @Query("SELECT um FROM user_memberships um JOIN FETCH um.membershipPackage WHERE um.status = 'ACTIVE' AND um.startDate <= :now AND um.endDate <= :now ORDER BY um.endDate ASC")
    List<UserMembership> findExpiredActiveMemberships(@Param("now") LocalDateTime now);

    @Query("SELECT um FROM user_memberships um JOIN FETCH um.membershipPackage WHERE um.status = 'ACTIVE' AND um.startDate <= :start AND um.endDate BETWEEN :start AND :end")
    List<UserMembership> findExpiringBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT um FROM user_memberships um JOIN FETCH um.membershipPackage WHERE um.userId = :userId ORDER BY um.endDate DESC")
    List<UserMembership> findByUserIdOrderByEndDateDesc(@Param("userId") String userId);

    boolean existsByUserIdAndStatus(String userId, MembershipStatus status);

    // Returns true when the user has ANY non-expired ACTIVE slot (current or queued).
    // Used as purchase guard — prevents buying a new membership when one already exists.
    @Query("SELECT COUNT(um) > 0 FROM user_memberships um WHERE um.userId = :userId AND um.status = 'ACTIVE' AND um.endDate > :now")
    boolean hasActiveMembership(@Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * Find the most recently expired membership for renewal eligibility check.
     * Returns a membership that expired after the given cutoff (now - 7 days).
     */
    @Query("SELECT um FROM user_memberships um WHERE um.userId = :userId AND um.status = 'EXPIRED' AND um.endDate > :cutoff ORDER BY um.endDate DESC")
    Optional<UserMembership> findMostRecentExpiredMembership(@Param("userId") String userId, @Param("cutoff") LocalDateTime cutoff);

    // ─── Admin Dashboard: Membership Distribution ───

    @Query(value = "SELECT mp.package_level, mp.package_name, COUNT(um.user_membership_id) AS member_count " +
            "FROM user_memberships um " +
            "JOIN membership_packages mp ON um.membership_id = mp.membership_id " +
            "WHERE um.status = 'ACTIVE' AND um.start_date <= :now AND um.end_date > :now " +
            "GROUP BY mp.package_level, mp.package_name " +
            "ORDER BY member_count DESC", nativeQuery = true)
    List<Object[]> countActiveGroupedByPackageLevel(@Param("now") LocalDateTime now);
}

