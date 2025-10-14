package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.AddressConversionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing address conversion mappings between old and new administrative structures
 */
@Repository
public interface AddressConversionMappingRepository extends JpaRepository<AddressConversionMapping, Long> {

    /**
     * Find conversion by exact old address (province + district + ward)
     *
     * @param oldProvinceId Province ID in old structure
     * @param oldDistrictId District ID in old structure
     * @param oldWardId Ward ID in old structure
     * @return Optional conversion mapping
     */
    Optional<AddressConversionMapping> findByOldProvinceProvinceIdAndOldDistrictDistrictIdAndOldWardWardIdAndIsActiveTrue(
            Long oldProvinceId, Long oldDistrictId, Long oldWardId);

    /**
     * Find all conversions related to a specific old province
     *
     * @param oldProvinceId Province ID in old structure
     * @return List of conversion mappings
     */
    List<AddressConversionMapping> findByOldProvinceProvinceIdAndIsActiveTrue(Long oldProvinceId);

    /**
     * Find all conversions related to a specific new province
     *
     * @param newProvinceId Province ID in new structure
     * @return List of conversion mappings
     */
    List<AddressConversionMapping> findByNewProvinceProvinceIdAndIsActiveTrue(Long newProvinceId);

    /**
     * Find all conversions where old province maps to new province
     *
     * @param oldProvinceId Province ID in old structure
     * @param newProvinceId Province ID in new structure
     * @return List of conversion mappings
     */
    List<AddressConversionMapping> findByOldProvinceProvinceIdAndNewProvinceProvinceIdAndIsActiveTrue(
            Long oldProvinceId, Long newProvinceId);

    /**
     * Find reverse conversion (new → old) for a given new address
     * Note: Multiple old addresses may map to the same new address
     *
     * @param newProvinceId Province ID in new structure
     * @param newWardId Ward ID in new structure
     * @return List of possible old addresses
     */
    List<AddressConversionMapping> findByNewProvinceProvinceIdAndNewWardWardIdAndIsActiveTrue(
            Long newProvinceId, Long newWardId);

    /**
     * Find all conversions for an old district
     *
     * @param oldDistrictId District ID in old structure
     * @return List of conversion mappings
     */
    List<AddressConversionMapping> findByOldDistrictDistrictIdAndIsActiveTrue(Long oldDistrictId);

    /**
     * Find all conversions for an old ward
     *
     * @param oldWardId Ward ID in old structure
     * @return List of conversion mappings
     */
    List<AddressConversionMapping> findByOldWardWardIdAndIsActiveTrue(Long oldWardId);

    /**
     * Find all conversions for a new ward
     *
     * @param newWardId Ward ID in new structure
     * @return List of conversion mappings
     */
    List<AddressConversionMapping> findByNewWardWardIdAndIsActiveTrue(Long newWardId);

    /**
     * Count total active conversions
     *
     * @return Number of active conversion mappings
     */
    long countByIsActiveTrue();

    /**
     * Find all conversions with specific accuracy threshold
     *
     * @param minAccuracy Minimum conversion accuracy percentage
     * @return List of conversion mappings with accuracy >= minAccuracy
     */
    @Query("SELECT m FROM address_conversion_mappings m " +
           "WHERE m.isActive = true AND m.conversionAccuracy >= :minAccuracy " +
           "ORDER BY m.conversionAccuracy DESC")
    List<AddressConversionMapping> findByMinimumAccuracy(@Param("minAccuracy") Integer minAccuracy);

    /**
     * Check if conversion exists for old address
     *
     * @param oldProvinceId Province ID in old structure
     * @param oldDistrictId District ID in old structure
     * @param oldWardId Ward ID in old structure
     * @return true if conversion exists
     */
    boolean existsByOldProvinceProvinceIdAndOldDistrictDistrictIdAndOldWardWardIdAndIsActiveTrue(
            Long oldProvinceId, Long oldDistrictId, Long oldWardId);

    /**
     * Get all conversion mappings (for batch operations or data export)
     *
     * @return List of all active conversion mappings
     */
    List<AddressConversionMapping> findByIsActiveTrueOrderByOldProvinceProvinceIdAscOldDistrictDistrictIdAscOldWardWardIdAsc();
}
