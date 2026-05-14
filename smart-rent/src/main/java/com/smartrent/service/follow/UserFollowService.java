package com.smartrent.service.follow;

import com.smartrent.dto.response.FollowStatusResponse;
import com.smartrent.dto.response.FollowedUserResponse;
import com.smartrent.infra.repository.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Directed user-to-user follow graph.
 *
 * "follower" subscribes to "following"'s activity. Today the only side-effect is a
 * notification when "following" publishes a new listing — see
 * {@link #notifyFollowersOfNewListing(Listing)}.
 */
public interface UserFollowService {

    /** Idempotent: re-following an already-followed target is a no-op. */
    FollowStatusResponse follow(String followerId, String followingId);

    /** Idempotent: unfollowing when no edge exists is a no-op. */
    FollowStatusResponse unfollow(String followerId, String followingId);

    /**
     * Status snapshot for one target. Pass a null/blank {@code followerId} to get
     * counts only (isFollowing=false). Used by anonymous detail views.
     */
    FollowStatusResponse getStatus(String followerId, String followingId);

    /**
     * Users who follow {@code userId}. Newest first.
     * {@code viewerId} (the authenticated caller, may be null) is used to populate
     * each row's {@code isFollowing} flag in bulk so the FE does not need N status
     * round-trips to render the follow toggle.
     */
    Page<FollowedUserResponse> getFollowers(String viewerId, String userId, Pageable pageable);

    /**
     * Users that {@code userId} follows. Newest first.
     * {@code viewerId} (the authenticated caller, may be null) is used to populate
     * each row's {@code isFollowing} flag in bulk.
     */
    Page<FollowedUserResponse> getFollowing(String viewerId, String userId, Pageable pageable);

    /**
     * Fan out a NEW_LISTING_FROM_FOLLOWED_USER notification to every follower of the
     * listing's owner. Async + best-effort; a failure here must never roll back the
     * listing transaction. Drafts and shadow listings are skipped at the call site.
     */
    void notifyFollowersOfNewListing(Listing listing);
}
