package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.AdministrativeStructure;
import com.smartrent.infra.repository.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    // ==================== EXISTING METHODS (BACKWARD COMPATIBLE) ====================

    // Get all active provinces (parent provinces only for dropdown)
    List<Province> findByParentProvinceIsNullAndIsActiveTrueOrderByName();

    // Get all provinces including merged ones
    List<Province> findByIsActiveTrueOrderByName();

    // Find by name (including merged provinces)
    List<Province> findByNameOrOriginalNameAndIsActiveTrue(String name, String originalName);

    // Find by code
    Optional<Province> findByCodeAndIsActiveTrue(String code);

    // Get merged provinces for a parent
    List<Province> findByParentProvinceProvinceIdAndIsActiveTrueOrderByName(Long parentId);

    // Search provinces by name (for autocomplete)
    List<Province> findByNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCaseAndIsActiveTrueOrderByName(
            String nameSearchTerm, String originalNameSearchTerm);

    // ==================== NEW METHODS FOR ADMINISTRATIVE STRUCTURE ====================

    /**
     * Find provinces by structure version
     *
     * @param structures List of structure versions to include (OLD, NEW, BOTH)
     * @return List of provinces matching the structure versions
     */
    @Query("SELECT p FROM provinces p WHERE p.structureVersion IN :structures AND p.isActive = true " +
           "AND p.parentProvince IS NULL ORDER BY p.name")
    List<Province> findByStructureVersionInAndIsActiveTrueOrderByName(
            @Param("structures") List<AdministrativeStructure> structures);

    /**
     * Find province by code and structure version
     *
     * @param code Province code
     * @param structures List of structure versions to include
     * @return Optional province
     */
    @Query("SELECT p FROM provinces p WHERE p.code = :code AND p.structureVersion IN :structures " +
           "AND p.isActive = true")
    Optional<Province> findByCodeAndStructureVersionInAndIsActiveTrue(
            @Param("code") String code,
            @Param("structures") List<AdministrativeStructure> structures);

    /**
     * Find provinces active at a specific date
     *
     * @param date The reference date
     * @return List of provinces active on that date
     */
    @Query("SELECT p FROM provinces p WHERE p.isActive = true " +
           "AND (p.effectiveFrom IS NULL OR p.effectiveFrom <= :date) " +
           "AND (p.effectiveTo IS NULL OR p.effectiveTo > :date) " +
           "ORDER BY p.name")
    List<Province> findActiveAtDate(@Param("date") LocalDate date);

    /**
     * Find provinces by structure version including merged ones
     *
     * @param structures List of structure versions
     * @return All provinces including merged
     */
    @Query("SELECT p FROM provinces p WHERE p.structureVersion IN :structures AND p.isActive = true " +
           "ORDER BY p.name")
    List<Province> findAllByStructureVersionInAndIsActiveTrueOrderByName(
            @Param("structures") List<AdministrativeStructure> structures);

    /**
     * Count provinces by structure version
     *
     * @param structure Structure version
     * @return Count of provinces
     */
    long countByStructureVersionAndIsActiveTrue(AdministrativeStructure structure);
}
