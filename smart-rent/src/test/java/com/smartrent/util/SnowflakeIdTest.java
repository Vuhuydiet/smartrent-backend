package com.smartrent.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdTest {

    @Test
    @DisplayName("ids are positive, unique and monotonically increasing within a JVM")
    void uniqueAndMonotonic() {
        Set<Long> seen = new HashSet<>();
        long prev = Long.MIN_VALUE;
        for (int i = 0; i < 200_000; i++) {
            long id = SnowflakeId.next();
            assertTrue(id > 0, "id must be positive");
            assertTrue(id > prev, "id must strictly increase");
            assertTrue(seen.add(id), "id must be unique");
            prev = id;
        }
    }

    @Test
    @DisplayName("no collisions under concurrent generation")
    void concurrentUnique() throws Exception {
        int threads = 16;
        int perThread = 20_000;
        Set<Long> all = ConcurrentHashMap.newKeySet();
        AtomicLong dupes = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    if (!all.add(SnowflakeId.next())) dupes.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(0, dupes.get(), "no duplicate ids under contention");
        assertEquals(threads * perThread, all.size());
    }
}
