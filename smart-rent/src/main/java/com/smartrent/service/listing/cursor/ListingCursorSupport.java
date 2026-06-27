package com.smartrent.service.listing.cursor;

import com.smartrent.infra.repository.entity.Listing;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Keyset (cursor) pagination support for the listing feed.
 *
 * <p>This is the cursor counterpart of {@code ListingQueryService.buildSort}: it
 * produces the SAME ordering, but as a list of typed keys so we can (a) build a
 * deterministic ORDER BY, (b) build a "seek" predicate {@code (row > cursor)},
 * and (c) encode/decode an opaque cursor token. {@code listingId DESC} is always
 * appended as the final, unique tiebreaker so the order is total and no row is
 * ever skipped or repeated across pages.
 *
 * <p>Non-null sort columns (vip_type_sort_order, updated_at, price, created_at,
 * listing_id) are used directly so the existing indexes still apply. The two
 * genuinely-nullable sort columns (area, post_date) are COALESCE-d to a sentinel
 * so they stay cursorable; those sorts have no dedicated index anyway, so the
 * coalesce costs nothing extra.
 */
public final class ListingCursorSupport {

    private ListingCursorSupport() {}

    public enum KeyType { INT, LONG, DECIMAL, DATETIME }

    /** One ORDER BY key. {@code attr} is the JPA attribute name on {@link Listing}. */
    public record CursorKey(String attr, boolean asc, KeyType type, boolean nullable) {}

    private static final CursorKey VIP = new CursorKey("vipTypeSortOrder", true, KeyType.INT, false);
    private static final CursorKey UPDATED_DESC = new CursorKey("updatedAt", false, KeyType.DATETIME, false);
    private static final CursorKey ID_TIEBREAK = new CursorKey("listingId", false, KeyType.LONG, false);

    /**
     * Build the ordered key list for a sortBy/sortDirection — mirrors
     * {@code ListingQueryService.buildSort} — with listingId appended as the
     * unique tiebreaker. The returned list IS the cursor: same list is used for
     * ordering, the seek predicate, and encode/decode.
     */
    public static List<CursorKey> keysFor(String sortBy, String sortDirection) {
        boolean asc = "ASC".equalsIgnoreCase(sortDirection);
        List<CursorKey> keys = new ArrayList<>();
        if (sortBy == null || sortBy.isEmpty()) {
            keys.add(VIP);
            keys.add(UPDATED_DESC);
        } else {
            switch (sortBy) {
                case "NEWEST" -> { keys.add(VIP); keys.add(UPDATED_DESC); }
                case "OLDEST" -> { keys.add(VIP); keys.add(new CursorKey("updatedAt", true, KeyType.DATETIME, false)); }
                case "PRICE_ASC" -> { keys.add(new CursorKey("price", true, KeyType.DECIMAL, false)); keys.add(VIP); keys.add(UPDATED_DESC); }
                case "PRICE_DESC" -> { keys.add(new CursorKey("price", false, KeyType.DECIMAL, false)); keys.add(VIP); keys.add(UPDATED_DESC); }
                case "price" -> { keys.add(new CursorKey("price", asc, KeyType.DECIMAL, false)); keys.add(VIP); keys.add(UPDATED_DESC); }
                case "area" -> { keys.add(new CursorKey("area", asc, KeyType.DECIMAL, true)); keys.add(VIP); keys.add(UPDATED_DESC); }
                case "createdAt" -> { keys.add(new CursorKey("createdAt", asc, KeyType.DATETIME, false)); keys.add(VIP); keys.add(UPDATED_DESC); }
                case "updatedAt" -> keys.add(new CursorKey("updatedAt", asc, KeyType.DATETIME, false));
                case "postDate" -> { keys.add(new CursorKey("postDate", asc, KeyType.DATETIME, true)); keys.add(VIP); keys.add(UPDATED_DESC); }
                default -> { keys.add(VIP); keys.add(UPDATED_DESC); } // includes "DEFAULT"
            }
        }
        keys.add(ID_TIEBREAK);
        return keys;
    }

    /** ORDER BY clause for the given keys. */
    public static List<Order> orders(CriteriaBuilder cb, Root<Listing> root, List<CursorKey> keys) {
        List<Order> orders = new ArrayList<>(keys.size());
        for (CursorKey k : keys) {
            Expression<?> e = expr(cb, root, k);
            orders.add(k.asc() ? cb.asc(e) : cb.desc(e));
        }
        return orders;
    }

    /**
     * Seek predicate: rows that come strictly AFTER the cursor row in the order.
     * Standard lexicographic OR-expansion:
     * (k0 ? v0) OR (k0=v0 AND k1 ? v1) OR ... where ? is &gt; for ASC, &lt; for DESC.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Specification<Listing> seek(List<CursorKey> keys, Object[] values) {
        return (root, query, cb) -> {
            List<Predicate> ors = new ArrayList<>(keys.size());
            for (int i = 0; i < keys.size(); i++) {
                List<Predicate> ands = new ArrayList<>(i + 1);
                for (int j = 0; j < i; j++) {
                    ands.add(cb.equal(expr(cb, root, keys.get(j)), values[j]));
                }
                CursorKey k = keys.get(i);
                Expression e = expr(cb, root, k);
                Comparable v = (Comparable) values[i];
                ands.add(k.asc() ? cb.greaterThan(e, v) : cb.lessThan(e, v));
                ors.add(cb.and(ands.toArray(new Predicate[0])));
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    /** Encode the cursor row's key values into an opaque token. */
    public static String encode(List<CursorKey> keys, Listing last) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(serialize(value(last, keys.get(i))));
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Decode a token back into typed values aligned with {@code keys}. */
    public static Object[] decode(String cursor, List<CursorKey> keys) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", -1);
        if (parts.length != keys.size()) {
            throw new IllegalArgumentException("Malformed cursor");
        }
        Object[] values = new Object[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            values[i] = parse(parts[i], keys.get(i).type());
        }
        return values;
    }

    // ── internals ──────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Expression<?> expr(CriteriaBuilder cb, Root<Listing> root, CursorKey k) {
        Expression<?> path = root.get(k.attr());
        if (k.nullable()) {
            return cb.coalesce((Expression) path, (Comparable) sentinel(k.type()));
        }
        return path;
    }

    private static Object value(Listing l, CursorKey k) {
        Object v = switch (k.attr()) {
            case "vipTypeSortOrder" -> l.getVipTypeSortOrder();
            case "updatedAt" -> l.getUpdatedAt();
            case "createdAt" -> l.getCreatedAt();
            case "postDate" -> l.getPostDate();
            case "price" -> l.getPrice();
            case "area" -> l.getArea();
            case "listingId" -> l.getListingId();
            default -> throw new IllegalStateException("Unknown cursor key: " + k.attr());
        };
        return (v == null && k.nullable()) ? sentinel(k.type()) : v;
    }

    private static String serialize(Object v) {
        return v == null ? "" : v.toString();
    }

    private static Object parse(String s, KeyType type) {
        return switch (type) {
            case INT -> Integer.valueOf(s);
            case LONG -> Long.valueOf(s);
            case DECIMAL -> new BigDecimal(s);
            case DATETIME -> LocalDateTime.parse(s);
        };
    }

    private static Comparable<?> sentinel(KeyType type) {
        return switch (type) {
            case INT -> 0;
            case LONG -> 0L;
            case DECIMAL -> BigDecimal.ZERO;
            case DATETIME -> LocalDateTime.of(1970, 1, 1, 0, 0);
        };
    }
}
