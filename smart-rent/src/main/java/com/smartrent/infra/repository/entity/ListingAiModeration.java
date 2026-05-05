package com.smartrent.infra.repository.entity;

import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity(name = "listing_ai_moderation")
@Table(name = "listing_ai_moderation",
       indexes = {
               @Index(name = "idx_listing_ai_moderation_status", columnList = "verification_status, manual_override")
       })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingAiModeration implements Persistable<Long> {

    @Id
    @Column(name = "listing_id")
    Long listingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    @Column(name = "ai_score")
    Double aiScore;

    @Column(name = "ai_reason", columnDefinition = "JSON")
    String aiReason;

    @Builder.Default
    @Column(name = "manual_override", nullable = false)
    Boolean manualOverride = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20)
    VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    Integer retryCount = 0;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return listingId;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
