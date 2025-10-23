package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Province;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    // ==================== LEGACY STRUCTURE QUERIES ====================

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

    // ==================== NEW 2025 STRUCTURE QUERIES ====================

    /**
     * Get all parent provinces (34 provinces in new structure) with pagination
     */
    @Query("SELECT p FROM provinces p WHERE p.parentProvince IS NULL AND p.isActive = true ORDER BY p.name")
    Page<Province> findNewProvinces(Pageable pageable);

    /**
     * Search parent provinces by keyword (name or code) with pagination
     */
    @Query("SELECT p FROM provinces p WHERE p.parentProvince IS NULL AND p.isActive = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.name")
    Page<Province> searchNewProvinces(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count parent provinces
     */
    @Query("SELECT COUNT(p) FROM provinces p WHERE p.parentProvince IS NULL AND p.isActive = true")
    long countNewProvinces();

    /**
     * Count parent provinces by keyword
     */
    @Query("SELECT COUNT(p) FROM provinces p WHERE p.parentProvince IS NULL AND p.isActive = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countNewProvincesByKeyword(@Param("keyword") String keyword);
}
