package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Street;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreetRepository extends JpaRepository<Street, Long> {

    // Get all streets by ward
    List<Street> findByWardWardIdAndIsActiveTrueOrderByName(Long wardId);

    // Search streets by name within a ward
    List<Street> findByNameContainingIgnoreCaseAndWardWardIdAndIsActiveTrueOrderByName(String searchTerm, Long wardId);

    // Get streets by district (across all wards)
    List<Street> findByWardDistrictDistrictIdAndIsActiveTrueOrderByName(Long districtId);

    // Find by name and ward (for validation)
    Optional<Street> findByNameAndWardWardIdAndIsActiveTrue(String name, Long wardId);
}
