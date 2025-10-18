package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Listing;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByListingIdIn(Collection<Long> listingIds);

    /**
     * Find all listings owned by a user
     *
     * @param userId The user ID
     * @return List of listings owned by the user
     */
    List<Listing> findByUserId(String userId);

    /**
     * Find shadow listing by parent (main) listing ID
     *
     * @param parentListingId The main listing ID
     * @return Optional containing the shadow listing if found
     */
    Optional<Listing> findByParentListingId(Long parentListingId);

    /**
     * Alias method for finding shadow listing by main listing ID
     *
     * @param mainListingId The main listing ID
     * @return Optional containing the shadow listing if found
     */
    default Optional<Listing> findShadowListingByMainListingId(Long mainListingId) {
        return findByParentListingId(mainListingId);
    }
}