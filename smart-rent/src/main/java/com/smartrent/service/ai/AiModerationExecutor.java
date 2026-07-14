package com.smartrent.service.ai;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated thread pools for AI-driven listing moderation.
 *
 * <p>Two isolated pools prevent the deadlock that would occur if the batch-level
 * parallelism and the per-listing fan-out shared a single pool:
 *
 * <ul>
 *   <li>{@link #batchPool()} — runs one task per listing in the scheduler batch (up to 20
 *       concurrent listings).  Each task blocks while waiting for the AI service to respond,
 *       so the pool is sized for I/O concurrency rather than CPU cores.</li>
 *   <li>{@link #taskPool()} — runs the verify + duplicate-check fan-out within each listing
 *       (2 tasks per listing). Kept separate to avoid the deadlock that would arise if
 *       batch-pool threads blocked waiting for sub-tasks queued on the same pool.</li>
 * </ul>
 *
 * <p>{@link ThreadPoolExecutor.CallerRunsPolicy} provides graceful back-pressure: when a pool
 * is saturated the calling thread executes the task directly, preventing queue overflow and
 * maintaining forward progress without throwing {@link java.util.concurrent.RejectedExecutionException}.
 * Daemon threads ensure neither pool blocks JVM shutdown.
 */
@Slf4j
@Component
public class AiModerationExecutor {

    private final ThreadPoolExecutor batchPool = new ThreadPoolExecutor(
            20, 20,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(40),
            r -> {
                Thread t = new Thread(r, "ai-mod-batch");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

    private final ThreadPoolExecutor taskPool = new ThreadPoolExecutor(
            16, 32,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128),
            r -> {
                Thread t = new Thread(r, "ai-mod-task");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

    /** For parallel processing of multiple listings in the scheduler batch. */
    public Executor batchPool() {
        return batchPool;
    }

    /** For parallel verify + duplicate-check fan-out within a single listing. */
    public Executor taskPool() {
        return taskPool;
    }

    @PreDestroy
    void shutdown() {
        shutdownPool(batchPool, "ai-mod-batch");
        shutdownPool(taskPool, "ai-mod-task");
    }

    private void shutdownPool(ThreadPoolExecutor pool, String name) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                log.warn("Thread pool '{}' did not terminate gracefully.", name);
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
