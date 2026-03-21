package com.smartrent.service.interestlevel.impl;

import com.smartrent.dto.response.InterestLevelResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.PhoneClickDetailRepository;
import com.smartrent.service.interestlevel.InterestLevelService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InterestLevelServiceImpl implements InterestLevelService {

    PhoneClickDetailRepository phoneClickDetailRepository;
    ListingRepository listingRepository;

    private static final int LOW_THRESHOLD = 3;
    private static final int MEDIUM_THRESHOLD = 10;
    private static final int HIGH_THRESHOLD = 25;

    @Override
    @Transactional(readOnly = true)
    public InterestLevelResponse getInterestLevel(Long listingId) {
        listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentClicks = phoneClickDetailRepository.countByListing_ListingIdAndClickedAtAfter(listingId, sevenDaysAgo);

        return mapToInterestLevel(recentClicks);
    }

    private InterestLevelResponse mapToInterestLevel(long recentClicks) {
        if (recentClicks >= HIGH_THRESHOLD) {
            return InterestLevelResponse.builder()
                    .level("TRENDING")
                    .label("Tin đăng này đang rất được quan tâm!")
                    .build();
        }
        if (recentClicks >= MEDIUM_THRESHOLD) {
            return InterestLevelResponse.builder()
                    .level("HIGH")
                    .label("Nhiều người đã liên hệ tin đăng này gần đây")
                    .build();
        }
        if (recentClicks >= LOW_THRESHOLD) {
            return InterestLevelResponse.builder()
                    .level("MEDIUM")
                    .label("Một số người quan tâm đến tin đăng này")
                    .build();
        }
        return InterestLevelResponse.builder()
                .level("LOW")
                .label("Hãy là người đầu tiên liên hệ!")
                .build();
    }
}
