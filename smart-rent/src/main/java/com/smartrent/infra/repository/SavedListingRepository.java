package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.SavedListing;
import com.smartrent.infra.repository.entity.SavedListingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedListingRepository extends JpaRepository<SavedListing, SavedListingId> {
    
    List<SavedListing> findByIdUserId(String userId);
    
    List<SavedListing> findByIdListingId(Long listingId);
    
    boolean existsByIdUserIdAndIdListingId(String userId, Long listingId);
    
    void deleteByIdUserIdAndIdListingId(String userId, Long listingId);
    
    @Query("SELECT DISTINCT sl FROM saved_listings sl " +
           "LEFT JOIN FETCH sl.user u " +
           "LEFT JOIN FETCH sl.listing l " +
           "LEFT JOIN FETCH l.address a " +
           "LEFT JOIN FETCH l.media m " +
           "LEFT JOIN FETCH l.amenities am " +
           "WHERE sl.id.userId = :userId " +
           "ORDER BY sl.createdAt DESC")
    List<SavedListing> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    @Query(value = "SELECT DISTINCT sl FROM saved_listings sl " +
           "LEFT JOIN FETCH sl.user u " +
           "LEFT JOIN FETCH sl.listing l " +
           "LEFT JOIN FETCH l.address a " +
           "WHERE sl.id.userId = :userId " +
           "ORDER BY sl.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT sl) FROM saved_listings sl WHERE sl.id.userId = :userId")
    org.springframework.data.domain.Page<SavedListing> findByUserIdWithDetails(
            @Param("userId") String userId,
            org.springframework.data.domain.Pageable pageable);

    long countByIdUserId(String userId);
}
