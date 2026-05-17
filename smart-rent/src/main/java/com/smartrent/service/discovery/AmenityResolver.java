package com.smartrent.service.discovery;

import com.smartrent.infra.repository.AmenityRepository;
import com.smartrent.infra.repository.entity.Amenity;
import com.smartrent.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the colloquial Vietnamese / English amenity phrases that the AI
 * parser and {@link com.smartrent.util.SearchQueryParser} emit (e.g.
 * {@code "máy lạnh"}, {@code "may lanh"}, {@code "ac"}, {@code "wifi"}) into the
 * canonical {@code amenities} table rows so natural-language search can filter
 * by amenity <em>id</em> exactly like the structured filter path does.
 *
 * <p>Before this existed, {@code ListingSpecification.matchesCriteria} matched
 * amenities with {@code LOWER(name) LIKE '%máy lạnh%'}. The DB stores the
 * canonical name ({@code "Điều hòa"}), so the LIKE never matched and the
 * amenity filter was silently dropped — a query like
 * "trọ tân bình có máy lạnh dưới 5tr" returned the same rows as
 * "trọ tân bình dưới 5tr".
 *
 * <h3>Why IDs come from the live table, not the alias map</h3>
 * The alias dictionary below is the single source of truth for
 * <em>language → canonical name</em>, but the {@code amenityId} is always read
 * from the live {@code amenities} table at resolve time. That keeps us in sync
 * with whatever the backend actually seeded (the AI service's
 * {@code amenities.json} carries a {@code _note} warning that its hardcoded ids
 * may drift from the backend).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmenityResolver {

    private final AmenityRepository amenityRepository;

    /** Rebuild the in-memory index at most this often. */
    private static final long INDEX_TTL_MILLIS = 10 * 60 * 1000L;

    /**
     * Curated {@code normalized-alias → canonical amenity name} dictionary,
     * ported from {@code smartrent-ai/app/agent/rag/knowledge_base/amenities.json}.
     * The canonical name is matched (accent-insensitively) against the live
     * {@code amenities.name} column to obtain the id, so a slightly different
     * stored spelling still resolves via name containment.
     */
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    private static void alias(String canonical, String... aliases) {
        ALIASES.put(TextNormalizer.normalize(canonical), canonical);
        for (String a : aliases) {
            String key = TextNormalizer.normalize(a);
            if (key != null && !key.isBlank()) {
                ALIASES.put(key, canonical);
            }
        }
    }

    static {
        alias("WiFi", "wifi", "internet", "mang", "wi-fi", "bang thong rong", "mạng");
        alias("Điều hòa", "dieu hoa", "may lanh", "máy lạnh", "air conditioning", "ac", "may dieu hoa");
        alias("Máy giặt", "may giat", "máy giặt", "washing machine", "giat");
        alias("Bãi đỗ xe", "bai do xe", "do xe", "cho dau xe", "cho de xe", "cho gui xe", "gui xe",
                "parking", "garage", "ham xe", "bãi đỗ xe", "chỗ để xe");
        alias("Bảo vệ", "bao ve", "an ninh", "security", "bao ve 24/7", "bao ve 24 24", "bảo vệ");
        alias("Thang máy", "thang may", "thang máy", "elevator", "lift");
        alias("Bếp", "bep", "kitchen", "nha bep", "phong bep", "bếp", "nhà bếp");
        alias("Ban công", "ban cong", "ban công", "balcony", "san thuong", "terrace");
        alias("Hồ bơi", "ho boi", "be boi", "swimming pool", "pool", "hồ bơi");
        alias("Phòng gym", "phong gym", "gym", "fitness", "tap gym", "phong tap", "phòng gym");
        alias("Tủ lạnh", "tu lanh", "tủ lạnh", "refrigerator", "fridge");
        alias("Máy nóng lạnh", "may nong lanh", "nuoc nong", "water heater", "heater");
        alias("Giường", "giuong", "giường", "bed", "giuong ngu");
        alias("Tivi", "tivi", "tv", "television", "man hinh", "ti vi");
        alias("Phòng khép kín", "phong khep kin", "khep kin", "toilet rieng", "wc rieng",
                "ve sinh rieng", "nha ve sinh rieng", "private bathroom", "en-suite", "phòng khép kín");
        alias("Sân vườn", "san vuon", "garden", "vuon", "yard", "sân vườn");
        alias("Camera an ninh", "camera", "cctv", "camera an ninh", "camera giam sat");
        alias("Bình nước nóng", "binh nuoc nong", "binh nong lanh", "may nuoc nong");
    }

    /** Outcome of a resolve call. */
    public record Resolved(Set<Long> amenityIds, List<String> unresolved) {
        public boolean isEmpty() {
            return amenityIds.isEmpty() && unresolved.isEmpty();
        }
    }

    // Lazily built, periodically refreshed: normalized key → amenityId.
    private volatile Map<String, Long> index = Map.of();
    private volatile long indexLoadedAt = 0L;

    /**
     * Resolves the given phrases to canonical amenity ids. Phrases that match no
     * known amenity are returned in {@link Resolved#unresolved()} so the caller
     * can keep the legacy LIKE behaviour for them and never regress recall.
     */
    public Resolved resolve(Collection<String> phrases) {
        Set<Long> ids = new LinkedHashSet<>();
        List<String> unresolved = new ArrayList<>();
        if (phrases == null || phrases.isEmpty()) {
            return new Resolved(ids, unresolved);
        }

        Map<String, Long> idx = currentIndex();
        for (String phrase : phrases) {
            if (phrase == null || phrase.isBlank()) {
                continue;
            }
            Long id = lookup(idx, phrase);
            if (id != null) {
                ids.add(id);
            } else {
                unresolved.add(phrase);
            }
        }
        return new Resolved(ids, unresolved);
    }

    private Long lookup(Map<String, Long> idx, String phrase) {
        String norm = TextNormalizer.normalize(phrase);
        if (norm == null || norm.isBlank()) {
            return null;
        }
        // 1. Exact normalized hit (covers canonical names and curated aliases).
        Long exact = idx.get(norm);
        if (exact != null) {
            return exact;
        }
        // 2. Bidirectional containment so "phong full noi that may lanh" still
        //    picks up "may lanh", and "wc" still picks up "wc rieng".
        Long best = null;
        int bestLen = 0;
        for (Map.Entry<String, Long> e : idx.entrySet()) {
            String key = e.getKey();
            if (key.length() < 2) {
                continue;
            }
            boolean hit = (" " + norm + " ").contains(" " + key + " ")
                    || norm.contains(key) || key.contains(norm);
            if (hit && key.length() > bestLen) {
                best = e.getValue();
                bestLen = key.length();
            }
        }
        return best;
    }

    private Map<String, Long> currentIndex() {
        long now = System.currentTimeMillis();
        Map<String, Long> snapshot = index;
        if (!snapshot.isEmpty() && (now - indexLoadedAt) < INDEX_TTL_MILLIS) {
            return snapshot;
        }
        synchronized (this) {
            if (!index.isEmpty() && (System.currentTimeMillis() - indexLoadedAt) < INDEX_TTL_MILLIS) {
                return index;
            }
            Map<String, Long> rebuilt = buildIndex();
            if (!rebuilt.isEmpty()) {
                index = rebuilt;
                indexLoadedAt = System.currentTimeMillis();
            }
            return index;
        }
    }

    /**
     * Builds {@code normalizedKey → amenityId} from the live table plus the
     * curated alias dictionary. Never throws: a failure here must degrade to an
     * empty index (caller then keeps the LIKE fallback) rather than break search.
     */
    private Map<String, Long> buildIndex() {
        Map<String, Long> idx = new LinkedHashMap<>();
        try {
            List<Amenity> amenities = amenityRepository.findAll();
            // a) direct: every active amenity is matchable by its own name.
            for (Amenity a : amenities) {
                if (a.getAmenityId() == null || a.getName() == null) {
                    continue;
                }
                if (Boolean.FALSE.equals(a.getIsActive())) {
                    continue;
                }
                String key = TextNormalizer.normalize(a.getName());
                if (key != null && !key.isBlank()) {
                    idx.putIfAbsent(key, a.getAmenityId());
                }
            }
            // b) aliases: map each curated alias onto the live id whose
            //    normalized name equals (or contains) the canonical name.
            for (Map.Entry<String, String> alias : ALIASES.entrySet()) {
                String canonicalNorm = TextNormalizer.normalize(alias.getValue());
                Long id = idForCanonical(idx, canonicalNorm);
                if (id != null) {
                    idx.putIfAbsent(alias.getKey(), id);
                }
            }
            log.debug("AmenityResolver index built: {} keys from {} amenities", idx.size(), amenities.size());
        } catch (Exception e) {
            log.warn("AmenityResolver index build failed (non-fatal, falling back to LIKE): {}", e.getMessage(), e);
        }
        return idx;
    }

    private Long idForCanonical(Map<String, Long> nameIndex, String canonicalNorm) {
        if (canonicalNorm == null || canonicalNorm.isBlank()) {
            return null;
        }
        Long exact = nameIndex.get(canonicalNorm);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Long> e : nameIndex.entrySet()) {
            if (e.getKey().contains(canonicalNorm) || canonicalNorm.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }
}