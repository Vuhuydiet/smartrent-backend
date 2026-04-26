package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records every call to the GET /v1/listings/search-suggestions endpoint.
 * Used for future popularity ranking of queries and dashboard analytics.
 */
@Entity
@Table(
    name = "search_query_impressions",
    indexes = {
        @Index(name = "idx_sqi_query_norm", columnList = "query_norm"),
        @Index(name = "idx_sqi_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchQueryImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    /** Normalized (TextNormalizer) version of the raw query string. */
    @Column(name = "query_norm", nullable = false, length = 256)
    String queryNorm;

    /** Legacy province ID string (may be numeric or new code). Nullable — no filter applied. */
    @Column(name = "province_id", length = 20)
    String provinceId;

    /** Category ID filter applied by the caller. Nullable. */
    @Column(name = "category_id")
    Long categoryId;

    /** Number of suggestions actually returned to the caller. */
    @Column(name = "suggestion_count", nullable = false)
    int suggestionCount;

    /** Best-effort client IP extracted from X-Forwarded-For or RemoteAddr. */
    @Column(name = "client_ip", length = 45)
    String clientIp;

    /**
     * Optional opaque session/device token supplied by the frontend.
     * Used to correlate impressions with clicks without requiring authentication.
     */
    @Column(name = "session_id", length = 64)
    String sessionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
