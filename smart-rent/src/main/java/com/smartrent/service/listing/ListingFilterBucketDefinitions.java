package com.smartrent.service.listing;

import java.util.List;

/**
 * Canonical bucket boundaries for the public listings sidebar's dynamic filters
 * (price / area / bedrooms). Single source of truth: the frontend's i18n labels
 * are keyed by {@link Bucket#key()}, so a bucket can only be renamed here and in
 * the frontend message files together — never in only one place.
 */
public final class ListingFilterBucketDefinitions {

    private ListingFilterBucketDefinitions() {
    }

    public record Bucket(String key, Long min, Long max) {
    }

    /**
     * VND (any {@code priceUnit}). Either bound may be {@code null} for an open
     * range. 3M-5M gets its own bucket (not folded into a wider 3-7M band) since
     * it's the single most common room-rental ("phòng trọ") budget in Vietnam.
     */
    public static final List<Bucket> PRICE = List.of(
            new Bucket("under3M", null, 3_000_000L),
            new Bucket("3to5M", 3_000_000L, 5_000_000L),
            new Bucket("5to10M", 5_000_000L, 10_000_000L),
            new Bucket("10to20M", 10_000_000L, 20_000_000L),
            new Bucket("over20M", 20_000_000L, null)
    );

    /**
     * m². Skewed toward the small end (phòng trọ are typically 15-40m²,
     * distinct from mini-apartments and houses) rather than an even split.
     */
    public static final List<Bucket> AREA = List.of(
            new Bucket("under20", null, 20L),
            new Bucket("20to35", 20L, 35L),
            new Bucket("35to60", 35L, 60L),
            new Bucket("60to100", 60L, 100L),
            new Bucket("over100", 100L, null)
    );

    public static final List<Bucket> BEDROOM = List.of(
            new Bucket("1", 1L, 1L),
            new Bucket("2to3", 2L, 3L),
            new Bucket("4to5", 4L, 5L),
            new Bucket("6plus", 6L, null)
    );
}
