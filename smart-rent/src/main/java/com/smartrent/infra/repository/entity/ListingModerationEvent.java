package com.smartrent.infra.repository.entity;

import com.smartrent.enums.ModerationAction;
import com.smartrent.enums.ModerationSource;
import com.smartrent.enums.ModerationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit trail for every moderation state transition on a listing.
 */
@Entity
@Table(name = "listing_moderation_events",
        indexes = {
                @Index(name = "idx_mod_event_listing", columnList = "listing_id"),
                @Index(name = "idx_mod_event_created", columnList = "created_at"),
                @Index(name = "idx_mod_event_admin", columnList = "admin_id")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingModerationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    Long eventId;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    ModerationSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    ModerationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    ModerationStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    ModerationAction action;

    @Column(name = "reason_code", length = 50)
    String reasonCode;

    @Column(name = "reason_text", columnDefinition = "TEXT")
    String reasonText;

    @Column(name = "admin_id", length = 36)
    String adminId;

    @Column(name = "triggered_by_user_id", length = 36)
    String triggeredByUserId;

    @Column(name = "report_id")
    Long reportId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
