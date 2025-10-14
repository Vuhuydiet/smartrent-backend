package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.AdministrativeStructure;
import com.smartrent.infra.repository.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, Long> {

    // ==================== EXISTING METHODS (BACKWARD COMPATIBLE) ====================

    // Get all wards by district
    List<Ward> findByDistrictDistrictIdAndIsActiveTrueOrderByName(Long districtId);

    // Find by code and district
    Optional<Ward> findByCodeAndDistrictDistrictIdAndIsActiveTrue(String code, Long districtId);

    // Search wards by name within a district
    List<Ward> findByNameContainingIgnoreCaseAndDistrictDistrictIdAndIsActiveTrueOrderByName(String searchTerm, Long districtId);

    // Get wards by province (across all districts)
    List<Ward> findByDistrictProvinceProvinceIdAndIsActiveTrueOrderByName(Long provinceId);

    // ==================== NEW METHODS FOR ADMINISTRATIVE STRUCTURE ====================

    /**
     * Find wards directly under a province by structure version (NEW structure)
     * In new structure, wards are directly under provinces (no districts)
     *
     * @param provinceId Province ID
     * @param structures List of structure versions to include
     * @return List of wards under the province
     */
    @Query("SELECT w FROM wards w WHERE w.district.province.provinceId = :provinceId " +
           "AND w.structureVersion IN :structures AND w.isActive = true " +
           "AND w.parentWard IS NULL ORDER BY w.name")
    List<Ward> findByProvinceIdAndStructureVersionInAndIsActiveTrueOrderByName(
            @Param("provinceId") Long provinceId,
            @Param("structures") List<AdministrativeStructure> structures);

    /**
     * Find parent wards (wards that have merged children) by structure version
     *
     * @param structures List of structure versions
     * @return List of parent wards
     */
    @Query("SELECT w FROM wards w WHERE w.structureVersion IN :structures AND w.isActive = true " +
           "AND w.parentWard IS NULL ORDER BY w.name")
    List<Ward> findByParentWardIsNullAndStructureVersionInAndIsActiveTrueOrderByName(
            @Param("structures") List<AdministrativeStructure> structures);

    /**
     * Find merged wards for a parent ward
     *
     * @param parentWardId Parent ward ID
     * @return List of merged wards
     */
    List<Ward> findByParentWardWardIdAndIsActiveTrueOrderByName(Long parentWardId);

    /**
     * Find ward by code and structure version
     *
     * @param code Ward code
     * @param structures List of structure versions
     * @return Optional ward
     */
    @Query("SELECT w FROM wards w WHERE w.code = :code AND w.structureVersion IN :structures " +
           "AND w.isActive = true")
    Optional<Ward> findByCodeAndStructureVersionInAndIsActiveTrue(
            @Param("code") String code,
            @Param("structures") List<AdministrativeStructure> structures);

    /**
     * Count wards by structure version
     *
     * @param structure Structure version
     * @return Count of wards
     */
    long countByStructureVersionAndIsActiveTrue(AdministrativeStructure structure);

    /**
     * Find wards that were merged (have parent ward)
     *
     * @return List of merged wards
     */
    List<Ward> findByParentWardIsNotNullAndIsActiveTrueOrderByName();

    /**
     * Search wards by name and structure version
     *
     * @param searchTerm Search term
     * @param structures List of structure versions
     * @return List of matching wards
     */
    @Query("SELECT w FROM wards w WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND w.structureVersion IN :structures AND w.isActive = true ORDER BY w.name")
    List<Ward> findByNameContainingIgnoreCaseAndStructureVersionInAndIsActiveTrueOrderByName(
            @Param("searchTerm") String searchTerm,
            @Param("structures") List<AdministrativeStructure> structures);

    /**
     * Find all wards by district and structure version
     *
     * @param districtId District ID
     * @param structures List of structure versions
     * @return List of wards
     */
    @Query("SELECT w FROM wards w WHERE w.district.districtId = :districtId " +
           "AND w.structureVersion IN :structures AND w.isActive = true ORDER BY w.name")
    List<Ward> findByDistrictIdAndStructureVersionInAndIsActiveTrueOrderByName(
            @Param("districtId") Long districtId,
            @Param("structures") List<AdministrativeStructure> structures);
}
