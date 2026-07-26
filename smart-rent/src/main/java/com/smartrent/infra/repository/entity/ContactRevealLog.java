package com.smartrent.infra.repository.entity;

import com.smartrent.enums.ContactRevealChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
 * Audit record of a single contact reveal: who ({@link #viewer}) revealed which
 * {@link #seller}'s contact, over which {@link #channel}, optionally from a
 * {@link #listing}, and when. Written on every authenticated reveal so contact
 * access can be tracked per user.
 */
@Entity(name = "contact_reveal_log")
@Table(name = "contact_reveal_log", indexes = {
        @Index(name = "idx_contact_reveal_viewer", columnList = "viewer_user_id, revealed_at"),
        @Index(name = "idx_contact_reveal_seller", columnList = "seller_user_id, revealed_at"),
        @Index(name = "idx_contact_reveal_listing", columnList = "listing_id")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContactRevealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_user_id", nullable = false)
    User viewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_user_id", nullable = false)
    User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 16, nullable = false)
    ContactRevealChannel channel;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "user_agent", length = 500)
    String userAgent;

    @Builder.Default
    @CreationTimestamp
    @Column(name = "revealed_at", updatable = false)
    LocalDateTime revealedAt = LocalDateTime.now();
}
