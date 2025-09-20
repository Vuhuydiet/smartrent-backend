package com.smartrent.infra.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingAmenityId implements Serializable {
    @Column(name = "listing_id")
    Integer listingId;

    @Column(name = "amenity_id")
    Integer amenityId;
}
