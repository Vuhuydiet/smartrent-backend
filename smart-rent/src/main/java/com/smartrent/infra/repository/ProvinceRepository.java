package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    // Get all active provinces (parent provinces only for dropdown)
    List<Province> findByParentProvinceIsNullAndIsActiveTrueOrderByName();

    // Get all provinces including merged ones
    List<Province> findByIsActiveTrueOrderByName();

    // Find by name (including merged provinces)
    List<Province> findByNameOrOriginalNameAndIsActiveTrue(String name, String originalName);

    // Find by code
    Optional<Province> findByCodeAndIsActiveTrue(String code);

    Optional<Province> findByCode(String code);

    // Get merged provinces for a parent
    List<Province> findByParentProvinceProvinceIdAndIsActiveTrueOrderByName(Long parentId);

    // Search provinces by name (for autocomplete)
    List<Province> findByNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCaseAndIsActiveTrueOrderByName(
            String nameSearchTerm, String originalNameSearchTerm);

    // Get all active provinces (simplified)
    List<Province> findByIsActiveTrue();
}
