package com.smartrent.cronjob;

import com.smartrent.service.membership.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs nightly at midnight to expire past-due memberships and grant benefits
 * to any queued memberships that became current (startDate now in the past).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipLifecycleScheduler {

    private final MembershipService membershipService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void processMembershipLifecycle() {
        log.info("MembershipLifecycleScheduler: starting nightly run");
        int expired = membershipService.expireOldMemberships();
        log.info("MembershipLifecycleScheduler: done — {} membership(s) expired", expired);
    }
}
