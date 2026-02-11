package com.smartrent.infra.repository.entity;

import com.smartrent.enums.OwnerActionStatus;
import com.smartrent.enums.OwnerActionTriggerType;
import com.smartrent.enums.OwnerActionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks owner obligations when admin requests a listing revision or resolves a report
 * requiring owner action.
 */
@Entity
@Table(name = "listing_owner_actions",
        indexes = {
                @Index(name = "idx_owner_action_listing", columnList = "listing_id"),
                @Index(name = "idx_owner_action_status", columnList = "status")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingOwnerAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_action_id")
    Long ownerActionId;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    OwnerActionTriggerType triggerType;

    @Column(name = "trigger_ref_id")
    Long triggerRefId;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_action", nullable = false, length = 30)
    OwnerActionType requiredAction;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    OwnerActionStatus status = OwnerActionStatus.PENDING_OWNER;

    @Column(name = "deadline_at")
    LocalDateTime deadlineAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
