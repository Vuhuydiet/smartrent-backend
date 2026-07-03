package com.smartrent.cronjob;

import com.smartrent.service.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every 5 minutes to cancel transactions that have been stuck in PENDING
 * for longer than the configured timeout (e.g. abandoned checkouts or missed
 * payment webhooks).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionPendingCancellationScheduler {

    private final TransactionService transactionService;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cancelStalePendingTransactions() {
        log.info("TransactionPendingCancellationScheduler: starting run");
        int cancelled = transactionService.cancelStalePendingTransactions();
        log.info("TransactionPendingCancellationScheduler: done — {} transaction(s) cancelled", cancelled);
    }
}
