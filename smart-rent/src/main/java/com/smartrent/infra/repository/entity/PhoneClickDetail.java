package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track when users click on phone numbers in listing details.
 * This helps renters understand which users are interested in their listings.
 */
@Entity(name = "phone_clicks")
@Table(name = "phone_clicks",
        indexes = {
                // idx_listing_id (⊂ idx_listing_user) + idx_user_id
                // (⊂ idx_phone_clicks_user_listing) dropped in V110.
                @Index(name = "idx_listing_user", columnList = "listing_id, user_id"),
                @Index(name = "idx_phone_clicks_user_listing", columnList = "user_id, listing_id"),
                @Index(name = "idx_clicked_at", columnList = "clicked_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhoneClickDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Builder.Default
    @CreationTimestamp
    @Column(name = "clicked_at", updatable = false)
    LocalDateTime clickedAt = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "user_agent", length = 500)
    String userAgent;
}

