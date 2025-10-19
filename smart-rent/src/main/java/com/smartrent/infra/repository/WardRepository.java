package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, Long> {

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
}
