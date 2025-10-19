package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    // Get all districts by province
    List<District> findByProvinceProvinceIdAndIsActiveTrueOrderByName(Long provinceId);

    // Get districts by parent province (for merged provinces)
    List<District> findByProvinceParentProvinceProvinceIdAndIsActiveTrueOrderByName(Long parentProvinceId);

    // Find by code and province
    Optional<District> findByCodeAndProvinceProvinceIdAndIsActiveTrue(String code, Long provinceId);

    Optional<District> findByCodeAndProvince_ProvinceId(String code, Long provinceId);

    Optional<District> findByCode(String code);

    // Search districts by name within a province (contains search)
    List<District> findByNameContainingIgnoreCaseAndProvinceProvinceIdAndIsActiveTrueOrderByName(String searchTerm, Long provinceId);

    // Search districts by name within parent province (for merged provinces)
    List<District> findByNameContainingIgnoreCaseAndProvinceParentProvinceProvinceIdAndIsActiveTrueOrderByName(String searchTerm, Long parentProvinceId);

    // Get all active districts
    List<District> findByIsActiveTrueOrderByName();

    List<District> findByIsActiveTrue();

    // Get districts by province (including active check)
    List<District> findByProvince_ProvinceIdAndIsActiveTrue(Long provinceId);
}
