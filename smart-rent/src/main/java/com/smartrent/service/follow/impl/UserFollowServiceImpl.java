package com.smartrent.service.follow.impl;

import com.smartrent.dto.response.FollowStatusResponse;
import com.smartrent.dto.response.FollowedUserResponse;
import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.UserFollowRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.infra.repository.entity.UserFollow;
import com.smartrent.service.follow.UserFollowService;
import com.smartrent.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FollowStatusResponse follow(String followerId, String followingId) {
        validateFollowPair(followerId, followingId);

        if (!userRepository.existsById(followingId)) {
            throw new AppException(DomainCode.USER_FOLLOW_TARGET_NOT_FOUND);
        }

        boolean alreadyFollowing = userFollowRepository
                .existsByFollowerIdAndFollowingId(followerId, followingId);
        if (!alreadyFollowing) {
            userFollowRepository.save(UserFollow.builder()
                    .followerId(followerId)
                    .followingId(followingId)
                    .build());
            log.info("User {} now follows {}", followerId, followingId);
        }

        return FollowStatusResponse.builder()
                .userId(followingId)
                .isFollowing(true)
                .followerCount(userFollowRepository.countByFollowingId(followingId))
                .build();
    }

    @Override
    @Transactional
    public FollowStatusResponse unfollow(String followerId, String followingId) {
        validateFollowPair(followerId, followingId);

        int deleted = userFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        if (deleted > 0) {
            log.info("User {} unfollowed {}", followerId, followingId);
        }

        return FollowStatusResponse.builder()
                .userId(followingId)
                .isFollowing(false)
                .followerCount(userFollowRepository.countByFollowingId(followingId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse getStatus(String followerId, String followingId) {
        if (followingId == null || followingId.isBlank()) {
            throw new AppException(DomainCode.USER_FOLLOW_TARGET_NOT_FOUND);
        }

        boolean isFollowing = followerId != null
                && !followerId.isBlank()
                && !followerId.equals(followingId)
                && userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);

        return FollowStatusResponse.builder()
                .userId(followingId)
                .isFollowing(isFollowing)
                .followerCount(userFollowRepository.countByFollowingId(followingId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowedUserResponse> getFollowers(String userId, Pageable pageable) {
        Page<UserFollow> page = userFollowRepository
                .findByFollowingIdOrderByCreatedAtDesc(userId, pageable);
        return mapEdgesToUsers(page, UserFollow::getFollowerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowedUserResponse> getFollowing(String userId, Pageable pageable) {
        Page<UserFollow> page = userFollowRepository
                .findByFollowerIdOrderByCreatedAtDesc(userId, pageable);
        return mapEdgesToUsers(page, UserFollow::getFollowingId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyFollowersOfNewListing(Listing listing) {
        if (listing == null
                || listing.getListingId() == null
                || listing.getUserId() == null
                || listing.getUserId().isBlank()
                || Boolean.TRUE.equals(listing.getIsShadow())
                || Boolean.TRUE.equals(listing.getIsDraft())) {
            return;
        }

        try {
            List<String> followerIds = userFollowRepository
                    .findFollowerIdsByFollowingId(listing.getUserId());
            if (followerIds.isEmpty()) {
                return;
            }

            String authorName = userRepository.findById(listing.getUserId())
                    .map(this::displayName)
                    .orElse("Người bạn theo dõi");
            String title = "Tin đăng mới từ " + authorName;
            String message = String.format(
                    "%s vừa đăng tin mới: \"%s\". Xem ngay để không bỏ lỡ cơ hội.",
                    authorName,
                    listing.getTitle() == null ? "(không tiêu đề)" : listing.getTitle());

            for (String followerId : followerIds) {
                if (followerId == null || followerId.isBlank()) continue;
                try {
                    notificationService.sendNotification(
                            followerId,
                            RecipientType.USER,
                            NotificationType.NEW_LISTING_FROM_FOLLOWED_USER,
                            title,
                            message,
                            listing.getListingId(),
                            "LISTING");
                } catch (Exception perFollower) {
                    // One bad recipient must not stop the rest of the fan-out.
                    log.warn("Failed to notify follower {} of listing {}: {}",
                            followerId, listing.getListingId(), perFollower.getMessage());
                }
            }
            log.info("Fanned out new-listing notification to {} followers of {} (listing {})",
                    followerIds.size(), listing.getUserId(), listing.getListingId());
        } catch (Exception e) {
            log.error("notifyFollowersOfNewListing failed for listing {}: {}",
                    listing.getListingId(), e.getMessage(), e);
        }
    }

    private void validateFollowPair(String followerId, String followingId) {
        if (followerId == null || followerId.isBlank()
                || followingId == null || followingId.isBlank()) {
            throw new AppException(DomainCode.USER_FOLLOW_TARGET_NOT_FOUND);
        }
        if (followerId.equals(followingId)) {
            throw new AppException(DomainCode.USER_FOLLOW_SELF_NOT_ALLOWED);
        }
    }

    private Page<FollowedUserResponse> mapEdgesToUsers(Page<UserFollow> page,
                                                       Function<UserFollow, String> idExtractor) {
        if (page.isEmpty()) {
            return page.map(edge -> null);
        }
        List<String> ids = page.getContent().stream()
                .map(idExtractor)
                .collect(Collectors.toList());
        Map<String, User> userMap = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        return page.map(edge -> {
            String otherId = idExtractor.apply(edge);
            User user = userMap.get(otherId);
            if (user == null) {
                return FollowedUserResponse.builder()
                        .userId(otherId)
                        .followedAt(edge.getCreatedAt())
                        .build();
            }
            return FollowedUserResponse.builder()
                    .userId(user.getUserId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatarUrl(user.getAvatarUrl())
                    .isBroker(user.isBroker())
                    .brokerVerificationStatus(user.getBrokerVerificationStatus() == null
                            ? null : user.getBrokerVerificationStatus().name())
                    .followedAt(edge.getCreatedAt())
                    .build();
        });
    }

    private String displayName(User u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String last = u.getLastName() == null ? "" : u.getLastName().trim();
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "Người bạn theo dõi" : combined;
    }
}
