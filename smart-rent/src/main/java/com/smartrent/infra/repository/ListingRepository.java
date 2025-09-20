package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Listing;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByListingIdIn(Collection<Long> listingIds);
}