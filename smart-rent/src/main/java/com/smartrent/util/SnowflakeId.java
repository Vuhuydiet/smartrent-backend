package com.smartrent.util;

import java.security.SecureRandom;

/**
 * Tiny Snowflake-style generator for application-assigned {@code long} ids.
 *
 * <p>Used so a {@code search_query_impressions} row can be given its id
 * <em>before</em> it is written, letting the write be dispatched
 * asynchronously while the synchronous request still returns a usable
 * {@code impressionId} for click-tracking. (The telemetry tables have no
 * foreign key — see {@code V72} — so an impression row landing slightly after
 * its id is handed out is fine.)
 *
 * <h3>Layout (63 usable bits, always positive)</h3>
 * <pre>
 *   bit 62           : 0 (sign — kept clear so the value is a positive long)
 *   bits 61‑22 (41)  : milliseconds since {@link #EPOCH}
 *   bits 21‑12 (10)  : per-JVM node, randomised at class load
 *   bits 11‑0  (12)  : per-millisecond sequence
 * </pre>
 * 41 bits of ms lasts ~69 years from {@link #EPOCH}. Within one JVM ids are
 * strictly increasing. Across JVMs a collision needs the same millisecond,
 * the same random 10-bit node (1/1024) AND the same 12-bit sequence — and the
 * only consequence is a duplicated analytics id (no integrity error), an
 * acceptable trade for keeping the write off the request path.
 */
public final class SnowflakeId {

    private SnowflakeId() {}

    /** 2024-01-01T00:00:00Z. */
    private static final long EPOCH = 1_704_067_200_000L;

    private static final int NODE_BITS = 10;
    private static final int SEQ_BITS = 12;
    private static final long MAX_SEQ = (1L << SEQ_BITS) - 1;
    private static final long NODE =
            new SecureRandom().nextInt(1 << NODE_BITS) & ((1L << NODE_BITS) - 1);

    private static long lastMs = -1L;
    private static long seq = 0L;

    /** Next unique, positive, monotonic (per JVM) id. */
    public static synchronized long next() {
        long now = System.currentTimeMillis();
        if (now == lastMs) {
            seq = (seq + 1) & MAX_SEQ;
            if (seq == 0) {
                // Sequence exhausted this ms — spin to the next ms.
                while ((now = System.currentTimeMillis()) <= lastMs) {
                    // busy-wait; sub-millisecond
                }
            }
        } else if (now < lastMs) {
            // Clock moved backwards — do not hand out an id that could collide
            // with an already-issued one; pin to lastMs and bump the sequence.
            seq = (seq + 1) & MAX_SEQ;
            now = lastMs;
        } else {
            seq = 0L;
        }
        lastMs = now;
        return ((now - EPOCH) << (NODE_BITS + SEQ_BITS))
                | (NODE << SEQ_BITS)
                | seq;
    }
}
