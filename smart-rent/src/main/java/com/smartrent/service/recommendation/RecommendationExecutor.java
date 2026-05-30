package com.smartrent.service.recommendation;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated pool for the recommendation multi-channel candidate fan-out
 * (proximity / price / fresh retrieval run as parallel CompletableFutures).
 *
 * <p>Why not {@code ForkJoinPool.commonPool()} (the default for
 * {@code CompletableFuture.supplyAsync} with no executor): the common pool's
 * parallelism is {@code cores - 1}, which is often 1 on a small prod box — so
 * the "parallel" channels actually run sequentially, and the blocking JDBC they
 * do can starve every other commonPool user. A small dedicated pool gives the
 * channels genuine parallelism regardless of core count and isolates the
 * blocking I/O. {@link ThreadPoolExecutor.CallerRunsPolicy} degrades gracefully
 * to running on the request thread under saturation; daemon threads never block
 * JVM shutdown.
 */
@Slf4j
@Component
public class RecommendationExecutor {

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            4, 8,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128),
            r -> {
                Thread t = new Thread(r, "reco-candidate");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());

    public Executor pool() {
        return pool;
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
