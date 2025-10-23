package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Ward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, Long> {

    // ==================== LEGACY STRUCTURE QUERIES ====================

    // Get all wards by district
    List<Ward> findByDistrictDistrictIdAndIsActiveTrueOrderByName(Long districtId);

    // Find by code and district
    Optional<Ward> findByCodeAndDistrictDistrictIdAndIsActiveTrue(String code, Long districtId);

    Optional<Ward> findByCodeAndDistrict_DistrictId(String code, Long districtId);

    Optional<Ward> findByCode(String code);

    // Search wards by name within a district
    List<Ward> findByNameContainingIgnoreCaseAndDistrictDistrictIdAndIsActiveTrueOrderByName(String searchTerm, Long districtId);

    // Get wards by province (across all districts)
    List<Ward> findByDistrictProvinceProvinceIdAndIsActiveTrueOrderByName(Long provinceId);

    // Get wards by district
    List<Ward> findByDistrict_DistrictIdAndIsActiveTrue(Long districtId);

    List<Ward> findByIsActiveTrue();

    // ==================== NEW 2025 STRUCTURE QUERIES ====================

    /**
     * Get wards directly under a province (2025 structure) with pagination
     */
    @Query("SELECT w FROM wards w WHERE w.province.code = :provinceCode " +
           "AND w.is2025Structure = true AND w.isActive = true ORDER BY w.name")
    Page<Ward> findWardsByNewProvinceCode(@Param("provinceCode") String provinceCode, Pageable pageable);

    /**
     * Search wards by keyword within a province (2025 structure) with pagination
     */
    @Query("SELECT w FROM wards w WHERE w.province.code = :provinceCode " +
           "AND w.is2025Structure = true AND w.isActive = true " +
           "AND (LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(w.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY w.name")
    Page<Ward> searchWardsByNewProvinceCode(@Param("provinceCode") String provinceCode,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);

    /**
     * Find ward by code in 2025 structure
     */
    @Query("SELECT w FROM wards w WHERE w.code = :wardCode " +
           "AND w.is2025Structure = true AND w.isActive = true")
    Optional<Ward> findNewWardByCode(@Param("wardCode") String wardCode);

    /**
     * Find ward by code and province code in 2025 structure
     */
    @Query("SELECT w FROM wards w WHERE w.code = :wardCode " +
           "AND w.province.code = :provinceCode " +
           "AND w.is2025Structure = true AND w.isActive = true")
    Optional<Ward> findNewWardByCodeAndProvinceCode(@Param("wardCode") String wardCode,
                                                     @Param("provinceCode") String provinceCode);

    /**
     * Search all wards and provinces by keyword (2025 structure) with pagination
     * Used for general address search
     */
    @Query("SELECT w FROM wards w WHERE w.is2025Structure = true AND w.isActive = true " +
           "AND (LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(w.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(w.province.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY w.province.name, w.name")
    Page<Ward> searchNewAddresses(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count wards by province code (2025 structure)
     */
    @Query("SELECT COUNT(w) FROM wards w WHERE w.province.code = :provinceCode " +
           "AND w.is2025Structure = true AND w.isActive = true")
    long countWardsByNewProvinceCode(@Param("provinceCode") String provinceCode);

    /**
     * Count wards by province code and keyword (2025 structure)
     */
    @Query("SELECT COUNT(w) FROM wards w WHERE w.province.code = :provinceCode " +
           "AND w.is2025Structure = true AND w.isActive = true " +
           "AND (LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(w.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countWardsByNewProvinceCodeAndKeyword(@Param("provinceCode") String provinceCode,
                                                @Param("keyword") String keyword);
}
