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

    @Query("SELECT um FROM user_memberships um WHERE um.userId = :userId AND um.status = 'ACTIVE' AND um.endDate > :now ORDER BY um.endDate DESC")
    Optional<UserMembership> findActiveUserMembership(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Query("SELECT um FROM user_memberships um WHERE um.status = 'ACTIVE' AND um.endDate <= :now")
    List<UserMembership> findExpiredMemberships(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE user_memberships um SET um.status = 'EXPIRED' WHERE um.status = 'ACTIVE' AND um.endDate <= :now")
    int expireOldMemberships(@Param("now") LocalDateTime now);

    boolean existsByUserIdAndStatus(String userId, MembershipStatus status);

    @Query("SELECT COUNT(um) > 0 FROM user_memberships um WHERE um.userId = :userId AND um.status = 'ACTIVE' AND um.endDate > :now")
    boolean hasActiveMembership(@Param("userId") String userId, @Param("now") LocalDateTime now);
}

