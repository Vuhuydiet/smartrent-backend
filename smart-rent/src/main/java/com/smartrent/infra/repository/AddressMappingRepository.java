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
     * Returns distinct (newProvinceCode, legacyProvince.id) pairs for the given new province codes.
     * Scans all rows (any ward level) — does NOT require province-level-only rows.
     * row[0] = newProvinceCode (String), row[1] = legacyProvince.id (Integer)
     */
    @Query("SELECT DISTINCT am.newProvinceCode, lp.id FROM AddressMapping am JOIN am.legacyProvince lp " +
            "WHERE am.newProvinceCode IN :newProvinceCodes")
    List<Object[]> findNewCodeToLegacyIdPairs(@Param("newProvinceCodes") List<String> newProvinceCodes);

    /**
     * Resolve a new ward code (optionally scoped by new province codes) into the
     * legacy ward IDs that map to it. Used by listing search to also include
     * old-structure listings when filtering by newWardCode.
     */
    @Query("SELECT DISTINCT lw.id FROM AddressMapping am JOIN am.legacyWard lw " +
            "WHERE am.newWardCode = :newWardCode " +
            "AND (:#{#newProvinceCodes == null || #newProvinceCodes.isEmpty()} = TRUE " +
            "     OR am.newProvinceCode IN :newProvinceCodes)")
    List<Integer> findLegacyWardIdsByNewWardCode(
            @Param("newWardCode") String newWardCode,
            @Param("newProvinceCodes") List<String> newProvinceCodes);

    /**
     * Returns distinct new province codes that map to the given legacy province codes.
     * Scans all rows (any ward level).
     * Used when FE sends old provinceIds and we need to resolve to new province codes.
     */
    @Query("SELECT DISTINCT am.newProvinceCode FROM AddressMapping am " +
            "WHERE am.legacyProvinceCode IN :legacyProvinceCodes")
    List<String> findNewProvinceCodesByLegacyProvinceCodes(@Param("legacyProvinceCodes") List<String> legacyProvinceCodes);

    /**
     * Returns distinct new province codes that map to the given legacy province IDs.
     * Joins AddressMapping → LegacyProvince on province_code, so we can pass the
     * legacy_provinces.id (auto-increment) directly from the search filter.
     * Used when FE sends old provinceId (legacy auto-id) in LEGACY mode and we
     * need new_province_code matches to also pick up listings stored under the
     * new structure.
     */
    @Query("SELECT DISTINCT am.newProvinceCode FROM AddressMapping am " +
            "JOIN am.legacyProvince lp " +
            "WHERE lp.id IN :legacyProvinceIds")
    List<String> findNewProvinceCodesByLegacyProvinceIds(@Param("legacyProvinceIds") List<Integer> legacyProvinceIds);

    /**
     * Returns distinct new ward codes that map to the given legacy ward IDs.
     * Joins AddressMapping → LegacyWard on ward_code, so we can pass the
     * legacy_wards.id (auto-increment) directly from the search filter.
     * Used when FE sends old wardId in LEGACY mode and we need new_ward_code
     * matches to also pick up listings stored under the new structure.
     */
    @Query("SELECT DISTINCT am.newWardCode FROM AddressMapping am " +
            "JOIN am.legacyWard lw " +
            "WHERE lw.id IN :legacyWardIds")
    List<String> findNewWardCodesByLegacyWardIds(@Param("legacyWardIds") List<Integer> legacyWardIds);

    /**
     * Returns distinct new ward codes that map to ANY legacy ward inside the
     * given legacy district. NEW administrative structure (2-tier) has no
     * district, so to honor a districtId filter against new-structure listings
     * we expand the district into its constituent ward codes.
     * Used when FE sends old districtId in LEGACY mode.
     */
    @Query("SELECT DISTINCT am.newWardCode FROM AddressMapping am " +
            "JOIN am.legacyDistrict ld " +
            "WHERE ld.id = :legacyDistrictId AND am.newWardCode IS NOT NULL")
    List<String> findNewWardCodesByLegacyDistrictId(@Param("legacyDistrictId") Integer legacyDistrictId);

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
