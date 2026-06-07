package com.smartrent.infra.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FavoriteId implements Serializable {

    // user_id is users.user_id VARCHAR(36) (UUID) — must be String, not Long.
    // Mapping it as Long made Hibernate read the column via getLong(), which
    // threw NumberFormatException when initializing the Listing.favorites
    // collection for any UUID user_id (e.g. AI auto-moderation of seeded data).
    @Column(name = "user_id")
    String userId;

    @Column(name = "listing_id")
    Long listingId;
}
