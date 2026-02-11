package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.AddressMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AddressMapping entity
 * Handles conversion between legacy (63 provinces) and new (34 provinces) address structure
 */
@Repository
public interface AddressMappingRepository extends JpaRepository<AddressMapping, Integer> {

    // ========== Legacy Province Mapping ==========

    /**
     * Find all mappings for a legacy province (may map to multiple new provinces if merged)
     */
    @Query("SELECT am FROM AddressMapping am WHERE am.legacyProvinceCode = :legacyProvinceCode")
    List<AddressMapping> findByLegacyProvinceCode(@Param("legacyProvinceCode") String legacyProvinceCode);

    /**
     * Find default mapping for a legacy province
     */
    @Query("SELECT am FROM AddressMapping am WHERE am.legacyProvinceCode = :legacyProvinceCode " +
            "AND am.legacyDistrictCode IS NULL AND am.legacyWardCode IS NULL " +
            "ORDER BY am.isMergedProvince ASC LIMIT 1")
    Optional<AddressMapping> findDefaultByLegacyProvinceCode(@Param("legacyProvinceCode") String legacyProvinceCode);

    // ========== Legacy Full Address Mapping ==========

    /**
     * Find all mappings for a complete legacy address (province + district + ward)
     */
    @Query("SELECT am FROM AddressMapping am WHERE " +
            "am.legacyProvinceCode = :provinceCode " +
            "AND am.legacyDistrictCode = :districtCode " +
            "AND am.legacyWardCode = :wardCode " +
            "ORDER BY am.isNewWardPolygonContainsWard DESC, am.isDefaultNewWard DESC, am.isNearestNewWard DESC")
    List<AddressMapping> findByLegacyAddress(
            @Param("provinceCode") String provinceCode,
            @Param("districtCode") String districtCode,
            @Param("wardCode") String wardCode);

    /**
     * Find default/best mapping for a complete legacy address
     * Priority: polygon contains > default > nearest
     */
    @Query("SELECT am FROM AddressMapping am WHERE " +
            "am.legacyProvinceCode = :provinceCode " +
            "AND am.legacyDistrictCode = :districtCode " +
            "AND am.legacyWardCode = :wardCode " +
            "ORDER BY am.isNewWardPolygonContainsWard DESC, am.isDefaultNewWard DESC, am.isNearestNewWard DESC " +
            "LIMIT 1")
    Optional<AddressMapping> findBestByLegacyAddress(
            @Param("provinceCode") String provinceCode,
            @Param("districtCode") String districtCode,
            @Param("wardCode") String wardCode);

    // ========== New Province Mapping ==========

    /**
     * Find all legacy provinces that map to a new province (reverse mapping)
     */
    @Query("SELECT am FROM AddressMapping am WHERE am.newProvinceCode = :newProvinceCode")
    List<AddressMapping> findByNewProvinceCode(@Param("newProvinceCode") String newProvinceCode);

    // ========== New Full Address Mapping ==========

    /**
     * Find all legacy addresses that map to a new address (reverse mapping)
     */
    @Query("SELECT am FROM AddressMapping am WHERE " +
            "am.newProvinceCode = :provinceCode " +
            "AND am.newWardCode = :wardCode " +
            "ORDER BY am.isDefaultNewWard DESC")
    List<AddressMapping> findByNewAddress(
            @Param("provinceCode") String provinceCode,
            @Param("wardCode") String wardCode);

    /**
     * Find default legacy address for a new address
     */
    @Query("SELECT am FROM AddressMapping am WHERE " +
            "am.newProvinceCode = :provinceCode " +
            "AND am.newWardCode = :wardCode " +
            "AND am.isDefaultNewWard = TRUE")
    Optional<AddressMapping> findDefaultByNewAddress(
            @Param("provinceCode") String provinceCode,
            @Param("wardCode") String wardCode);

    // ========== Special Mapping Types ==========

    /**
     * Find all merged provinces
     */
    @Query("SELECT DISTINCT am FROM AddressMapping am WHERE am.isMergedProvince = TRUE")
    List<AddressMapping> findMergedProvinces();

    /**
     * Find all merged wards
     */
    @Query("SELECT am FROM AddressMapping am WHERE am.isMergedWard = TRUE")
    List<AddressMapping> findMergedWards();

    /**
     * Find all divided wards (1 legacy ward → multiple new wards)
     */
    @Query("SELECT am FROM AddressMapping am WHERE am.isDividedWard = TRUE " +
            "ORDER BY am.legacyWardCode, am.isDefaultNewWard DESC")
    List<AddressMapping> findDividedWards();

    /**
     * Find all mappings for a legacy ward that was divided
     */
    @Query("SELECT am FROM AddressMapping am WHERE " +
            "am.legacyWardCode = :legacyWardCode " +
            "AND am.isDividedWard = TRUE " +
            "ORDER BY am.isDefaultNewWard DESC")
    List<AddressMapping> findDivisionsByLegacyWard(@Param("legacyWardCode") String legacyWardCode);
}
