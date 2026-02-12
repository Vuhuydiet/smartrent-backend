package com.smartrent.infra.repository;

import com.smartrent.enums.OwnerActionStatus;
import com.smartrent.infra.repository.entity.ListingOwnerAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingOwnerActionRepository extends JpaRepository<ListingOwnerAction, Long> {

    List<ListingOwnerAction> findByListingIdAndStatus(Long listingId, OwnerActionStatus status);

    Optional<ListingOwnerAction> findFirstByListingIdOrderByCreatedAtDesc(Long listingId);

    List<ListingOwnerAction> findByListingIdOrderByCreatedAtDesc(Long listingId);
}
