package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.View;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {

    long countByListing_ListingId(Long listingId);
}
