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
    
    @Query("SELECT sl FROM saved_listings sl WHERE sl.id.userId = :userId ORDER BY sl.createdAt DESC")
    List<SavedListing> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    long countByIdUserId(String userId);
}
