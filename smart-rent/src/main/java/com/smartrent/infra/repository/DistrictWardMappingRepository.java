package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.DistrictWardMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DistrictWardMapping entity
 * Maps legacy districts to new wards (districts were eliminated in new structure)
 */
@Repository
public interface DistrictWardMappingRepository extends JpaRepository<DistrictWardMapping, Integer> {

    /**
     * Find district-ward mappings by legacy district ID
     * @param districtId Legacy district ID
     * @return List of mappings for the district
     */
    List<DistrictWardMapping> findByLegacyDistrictId(Integer districtId);

    /**
     * Find district-ward mappings by new ward code
     * Used for reverse conversion: new ward -> legacy district
     * @param newWardCode New ward code
     * @return List of mappings for the ward
     */
    @Query("SELECT dwm FROM DistrictWardMapping dwm WHERE dwm.newWard.code = :newWardCode")
    List<DistrictWardMapping> findByNewWardCode(@Param("newWardCode") String newWardCode);

    /**
     * Find single district-ward mapping by legacy district ID
     * @param districtId Legacy district ID
     * @return Optional mapping
     */
    Optional<DistrictWardMapping> findFirstByLegacyDistrictId(Integer districtId);

    /**
     * Find single district-ward mapping by new ward code
     * @param newWardCode New ward code
     * @return Optional mapping
     */
    @Query("SELECT dwm FROM DistrictWardMapping dwm WHERE dwm.newWard.code = :newWardCode")
    Optional<DistrictWardMapping> findFirstByNewWardCode(@Param("newWardCode") String newWardCode);
}