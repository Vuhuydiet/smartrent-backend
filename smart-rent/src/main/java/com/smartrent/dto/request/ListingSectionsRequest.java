package com.smartrent.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request for {@code POST /v1/listings/search/sections} — fetch several VIP-tier
 * carousels (DIAMOND / GOLD / SILVER / NORMAL) in a single round-trip instead of
 * one {@code POST /search} per tier.
 *
 * <p>{@code verified}, {@code page} and {@code size} are shared defaults applied
 * to every section; a section may override {@code size} and add a {@code sortBy}.
 * Each section maps 1:1 onto an ordinary {@link ListingFilterRequest} and runs
 * through the very same {@code searchListings} path (and cache), so the promoted
 * ("đẩy tin") ordering is byte-identical to the per-tier calls it replaces.
 *
 * <p>Geo coordinates are intentionally absent: tier carousels are not ranked by
 * distance, so sending user coordinates only fragmented the cache for no effect.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingSectionsRequest {

    /** Applied to every section (homepage sends {@code true} to show verified tins only). */
    Boolean verified;

    /** 1-based page shared by every section (defaults to 1 when null/invalid). */
    Integer page;

    /** Default page size shared by every section (defaults to 10 when null/invalid). */
    Integer size;

    /** Ordered tier sections to fetch. */
    List<Section> sections;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Section {

        /** NORMAL / SILVER / GOLD / DIAMOND. */
        String vipType;

        /** Optional sort override (e.g. {@code NEWEST}); null keeps the default VIP-tier sort. */
        String sortBy;

        /** Optional per-section size override; falls back to the shared size when null. */
        Integer size;
    }
}
