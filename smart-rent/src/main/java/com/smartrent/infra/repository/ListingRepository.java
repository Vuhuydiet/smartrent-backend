package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Listing;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByListingIdIn(Collection<Long> listingIds);

    @Query("SELECT l FROM listings l LEFT JOIN FETCH l.amenities WHERE l.listingId = :id")
    Optional<Listing> findByIdWithAmenities(@Param("id") Long id);

    @Query("SELECT l FROM listings l LEFT JOIN FETCH l.amenities WHERE l.listingId IN :ids")
    List<Listing> findByIdsWithAmenities(@Param("ids") Collection<Long> ids);
}