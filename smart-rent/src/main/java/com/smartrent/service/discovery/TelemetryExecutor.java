package com.smartrent.service.discovery;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Small dedicated pool for fire-and-forget search telemetry writes so the
 * suggestion request thread never waits on a DB INSERT.
 *
 * <p>Deliberately tiny and bounded with a {@link ThreadPoolExecutor.CallerRunsPolicy}:
 * if the pool and queue are saturated the task runs on the calling thread —
 * i.e. it gracefully degrades to the old synchronous behaviour instead of
 * dropping telemetry or throwing into the request. A daemon thread factory
 * keeps it from blocking JVM shutdown.
 */
@Slf4j
@Component
public class TelemetryExecutor {

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            1, 2,
            30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(512),
            r -> {
                Thread t = new Thread(r, "telemetry-writer");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

    /** Submit a telemetry task. Never throws to the caller. */
    public void execute(Runnable task) {
        try {
            pool.execute(task);
        } catch (Exception e) {
            // CallerRunsPolicy makes this practically unreachable; still never
            // let a telemetry dispatch failure surface into the request.
            log.warn("telemetry: dispatch failed (non-fatal): {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
