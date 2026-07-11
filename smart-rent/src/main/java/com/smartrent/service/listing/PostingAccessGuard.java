package com.smartrent.service.listing;

import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Central guard that rejects listing-posting actions for users an admin has
 * blocked from posting (too many admin-approved reports). Used by every entry
 * point that creates or re-lists a listing (create, VIP create, publish draft
 * via create, repost) so the block is enforced uniformly at the backend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostingAccessGuard {

    UserRepository userRepository;

    /**
     * Throws {@link DomainCode#USER_POSTING_BLOCKED} when the user is blocked from posting.
     */
    public void ensureCanPost(String userId) {
        if (userId == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> {
            if (user.isPostingBlocked()) {
                log.warn("Blocked user {} attempted a posting action", userId);
                throw new DomainException(DomainCode.USER_POSTING_BLOCKED);
            }
        });
    }
}
