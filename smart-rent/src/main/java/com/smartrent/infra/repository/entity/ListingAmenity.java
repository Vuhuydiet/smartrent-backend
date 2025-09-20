package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "listing_amenities",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_listing_amenity", columnNames = {"listing_id", "amenity_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingAmenity {

    @EmbeddedId
    ListingAmenityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("listingId")
    @JoinColumn(name = "listing_id", insertable = false, updatable = false)
    Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("amenityId")
    @JoinColumn(name = "amenity_id", insertable = false, updatable = false)
    Amenity amenity;
}
