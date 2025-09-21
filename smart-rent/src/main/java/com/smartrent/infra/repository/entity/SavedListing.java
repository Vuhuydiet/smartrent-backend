package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity(name = "saved_listings")
@Table(name = "saved_listings",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_listing_saved", columnNames = {"user_id", "listing_id"})
        },
        indexes = {
                @Index(name = "idx_saved_user_id", columnList = "user_id"),
                @Index(name = "idx_saved_listing_id", columnList = "listing_id")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SavedListing {

    @EmbeddedId
    SavedListingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("listingId")
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;
}
