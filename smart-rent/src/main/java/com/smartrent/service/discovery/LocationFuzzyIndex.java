package com.smartrent.service.discovery;

import com.smartrent.infra.repository.LegacyDistrictRepository;
import com.smartrent.infra.repository.LegacyProvinceRepository;
import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.LegacyProvince;
import com.smartrent.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Typo-tolerant resolver for province / district names, used as a fallback
 * when the exact (LIKE / key) location tiers find nothing — e.g. the user
 * typed "tn binh" instead of "tân bình".
 *
 * <h3>Why in-memory</h3>
 * There are only ~63 provinces + ~700 districts; their names are tiny
 * strings and change about once a decade. Loading them once into a cached
 * list and scoring with Jaro-Winkler is sub-millisecond and needs zero DB
 * round-trips on the request path — far cheaper and more controllable than
 * an ngram FULLTEXT scan, and it never blocks the dropdown.
 *
 * <h3>Freshness</h3>
 * The index self-heals: it is rebuilt lazily when older than
 * {@link #REFRESH_TTL_MS} (the legacy admin tables are effectively static,
 * so a long TTL is safe and keeps the hot path allocation-free).
 *
 * <p>The pure {@link #buildIndex} / {@link #searchIndex} helpers are
 * package-private and Spring-free so the scoring can be unit-tested without
 * a database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationFuzzyIndex {

    /** Shared, thread-safe (stateless) similarity scorer. */
    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();

    /** Rebuild the cached index when it is older than this (6 hours). */
    private static final long REFRESH_TTL_MS = 6L * 60 * 60 * 1000;

    /** Default similarity floor — below this a "match" is just noise. */
    public static final double DEFAULT_MIN_SCORE = 0.86;

    /** Fuzzy matching on 1-2 char fragments is meaningless; require ≥ this. */
    private static final int MIN_QUERY_LENGTH = 3;

    private final LegacyProvinceRepository provinceRepository;
    private final LegacyDistrictRepository districtRepository;

    private volatile Index cached;
    private volatile long builtAtMs;

    public enum Kind { PROVINCE, DISTRICT }

    /** One searchable place plus the legacy ids the frontend needs. */
    public record Entry(
            Kind kind,
            String display,
            Integer legacyProvinceId,
            Integer legacyDistrictId,
            String provinceCode,
            String districtCode,
            String provinceName,
            String districtName,
            List<String> matchStrings) {}

    public record Index(List<Entry> entries) {}

    /** A scored fuzzy hit (highest score first when returned). */
    public record Match(
            Kind kind,
            String display,
            Integer legacyProvinceId,
            Integer legacyDistrictId,
            String provinceCode,
            String districtCode,
            String provinceName,
            String districtName,
            double score) {}

    /**
     * Best fuzzy province/district matches for an already-normalized query
     * (accent-stripped, lowercase — i.e. {@link TextNormalizer#normalize}
     * output). Returns an empty list (never throws) on any failure so the
     * suggestion response degrades gracefully.
     */
    public List<Match> search(String normalizedQuery, double minScore, int limit) {
        try {
            return searchIndex(index(), normalizedQuery, minScore, limit);
        } catch (Exception e) {
            log.warn("location-fuzzy: search failed (non-fatal): {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Exact (normalized) district lookup scoped to a province — used to
     * resolve the legacy ids behind curated "default" suggestions without a
     * DB round-trip (the index is already in memory). {@code normalizedName}
     * must be {@link TextNormalizer#normalize} output, e.g. {@code "quan 1"}
     * or {@code "binh thanh"}.
     */
    public Optional<Match> resolveDistrict(String provinceCode, String normalizedName) {
        try {
            return resolveDistrictIn(index(), provinceCode, normalizedName);
        } catch (Exception ex) {
            log.warn("location-fuzzy: resolveDistrict failed (non-fatal): {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private Index index() {
        Index local = cached;
        if (local != null && System.currentTimeMillis() - builtAtMs <= REFRESH_TTL_MS) {
            return local;
        }
        synchronized (this) {
            if (cached == null || System.currentTimeMillis() - builtAtMs > REFRESH_TTL_MS) {
                cached = buildIndex(provinceRepository.findAll(), districtRepository.findAll());
                builtAtMs = System.currentTimeMillis();
                log.debug("location-fuzzy: index built with {} entries", cached.entries().size());
            }
            return cached;
        }
    }

    // ── Pure, Spring-free core (unit-testable) ───────────────────────────────

    static Index buildIndex(List<LegacyProvince> provinces, List<District> districts) {
        Map<String, LegacyProvince> byCode = new HashMap<>();
        List<Entry> entries = new ArrayList<>();

        for (LegacyProvince p : provinces) {
            if (p.getCode() != null) byCode.put(p.getCode(), p);
            if (p.getId() == null) continue;
            List<String> match = norms(p.getName(), p.getShortName(), p.getAlias());
            if (match.isEmpty()) continue;
            entries.add(new Entry(Kind.PROVINCE, p.getName(), p.getId(), null,
                    p.getCode(), null, p.getName(), null, match));
        }

        for (District d : districts) {
            if (d.getId() == null || d.getProvinceCode() == null) continue;
            LegacyProvince parent = byCode.get(d.getProvinceCode());
            Integer provinceId = parent != null ? parent.getId() : null;
            if (provinceId == null) continue; // can't filter without a province id
            List<String> match = norms(d.getName(), d.getShortName(), d.getAlias());
            if (match.isEmpty()) continue;
            entries.add(new Entry(Kind.DISTRICT, d.getName(), provinceId, d.getId(),
                    d.getProvinceCode(), d.getCode(), d.getProvinceName(), d.getName(), match));
        }
        return new Index(entries);
    }

    private static List<String> norms(String... raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String r : raw) {
            String n = TextNormalizer.normalize(r);
            if (n != null && !n.isBlank()) out.add(n);
        }
        return new ArrayList<>(out);
    }

    static List<Match> searchIndex(Index idx, String normalizedQuery, double minScore, int limit) {
        if (idx == null || normalizedQuery == null) return List.of();
        String query = normalizedQuery.trim();
        if (query.length() < MIN_QUERY_LENGTH || limit <= 0) return List.of();

        int qLen = query.length();
        // Loose length gate so a typo can drop/add a few chars but a wildly
        // different-length name is skipped before the O(n*m) scorer runs.
        int maxLenDelta = Math.max(4, qLen);

        List<Match> hits = new ArrayList<>();
        for (Entry e : idx.entries()) {
            double best = 0.0;
            for (String candidate : e.matchStrings()) {
                if (Math.abs(candidate.length() - qLen) > maxLenDelta) continue;
                Double score = JARO_WINKLER.apply(query, candidate);
                if (score != null && score > best) best = score;
            }
            if (best >= minScore) {
                hits.add(toMatch(e, best));
            }
        }

        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        // Dedup by display text; the list is already best-score-first so the
        // first occurrence wins.
        Map<String, Match> deduped = new LinkedHashMap<>();
        for (Match m : hits) deduped.putIfAbsent(m.display().toLowerCase(), m);

        List<Match> out = new ArrayList<>(deduped.values());
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    static Optional<Match> resolveDistrictIn(
            Index idx, String provinceCode, String normalizedName) {
        if (idx == null || provinceCode == null || normalizedName == null) {
            return Optional.empty();
        }
        String q = normalizedName.trim();
        if (q.isEmpty()) return Optional.empty();
        for (Entry e : idx.entries()) {
            if (e.kind() == Kind.DISTRICT
                    && provinceCode.equals(e.provinceCode())
                    && e.matchStrings().contains(q)) {
                return Optional.of(toMatch(e, 1.0));
            }
        }
        return Optional.empty();
    }

    private static Match toMatch(Entry e, double score) {
        return new Match(e.kind(), e.display(), e.legacyProvinceId(),
                e.legacyDistrictId(), e.provinceCode(), e.districtCode(),
                e.provinceName(), e.districtName(), score);
    }
}
