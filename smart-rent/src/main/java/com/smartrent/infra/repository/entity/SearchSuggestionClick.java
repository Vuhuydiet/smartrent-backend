package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records a user selecting (clicking) one suggestion from the search-suggestions result list.
 * Used to rank popular queries and titles for future weighted scoring.
 * <p>
 * {@code impression_id} is a soft reference — no FK constraint so inserts
 * stay fast and survive race conditions with the impression write.
 */
@Entity
@Table(
    name = "search_suggestion_clicks",
    indexes = {
        @Index(name = "idx_ssc_impression_id", columnList = "impression_id"),
        @Index(name = "idx_ssc_type_text",     columnList = "suggestion_type, suggestion_text"),
        @Index(name = "idx_ssc_created_at",    columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchSuggestionClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    /**
     * Soft reference to {@link SearchQueryImpression#getId()}.
     * Nullable to allow decoupled / late click events.
     */
    @Column(name = "impression_id")
    Long impressionId;

    /**
     * Which source produced this suggestion: TITLE | LOCATION | POPULAR_QUERY.
     * Stored as a plain VARCHAR to avoid tight coupling with the Java enum.
     */
    @Column(name = "suggestion_type", nullable = false, length = 20)
    String suggestionType;

    /** The full display text of the clicked suggestion. */
    @Column(name = "suggestion_text", nullable = false, length = 512)
    String suggestionText;

    /**
     * Listing ID for TITLE suggestions, null for LOCATION / POPULAR_QUERY.
     * Allows navigating directly to the listing without a secondary search.
     */
    @Column(name = "listing_id")
    Long listingId;

    /** 0-indexed position of this suggestion in the returned list (for position-bias analysis). */
    @Column(name = "rank_position", nullable = false)
    int rankPosition;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
