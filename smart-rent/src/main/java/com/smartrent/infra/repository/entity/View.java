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

@Entity(name = "views")
@Table(name = "views",
        indexes = {
                // idx_listing_id dropped in V110 — prefix of idx_listing_time.
                @Index(name = "idx_listing_time", columnList = "listing_id, viewed_at"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_ip_time", columnList = "ip_address, viewed_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class View {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    Listing listing;

    @Column(name = "user_id", length = 36) // Can be null for anonymous views
    String userId; // Reference to User.userId (UUID), matches users.user_id VARCHAR(36)

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "user_agent", length = 500)
    String userAgent;

    @Builder.Default
    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    LocalDateTime viewedAt = LocalDateTime.now();
}