package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    @Modifying
    @Query("DELETE FROM user_follows uf WHERE uf.followerId = :followerId AND uf.followingId = :followingId")
    int deleteByFollowerIdAndFollowingId(@Param("followerId") String followerId,
                                         @Param("followingId") String followingId);

    long countByFollowingId(String followingId);

    long countByFollowerId(String followerId);

    /** Stream-friendly: used to fan out new-listing notifications to all followers. */
    @Query("SELECT uf.followerId FROM user_follows uf WHERE uf.followingId = :followingId")
    List<String> findFollowerIdsByFollowingId(@Param("followingId") String followingId);

    /**
     * IDs of users {@code followerId} is following. Powers the "from users I follow"
     * listing feed.
     */
    @Query("SELECT uf.followingId FROM user_follows uf WHERE uf.followerId = :followerId")
    List<String> findFollowingIdsByFollowerId(@Param("followerId") String followerId);

    Page<UserFollow> findByFollowingIdOrderByCreatedAtDesc(String followingId, Pageable pageable);

    Page<UserFollow> findByFollowerIdOrderByCreatedAtDesc(String followerId, Pageable pageable);

    /**
     * Bulk follow-status check for a list of target users — used to annotate listing/search
     * results with "isFollowed" without N round-trips.
     */
    @Query("SELECT uf.followingId FROM user_follows uf " +
           "WHERE uf.followerId = :followerId AND uf.followingId IN :followingIds")
    List<String> findFollowedTargetIds(@Param("followerId") String followerId,
                                       @Param("followingIds") Collection<String> followingIds);
}
