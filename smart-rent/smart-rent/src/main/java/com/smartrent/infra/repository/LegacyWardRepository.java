package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.LegacyWard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegacyWardRepository extends JpaRepository<LegacyWard, Integer> {

    @Query("SELECT w FROM LegacyWard w WHERE w.districtCode = :districtCode")
    Page<LegacyWard> findByDistrictCode(@Param("districtCode") String districtCode, Pageable pageable);

    @Query("SELECT w FROM LegacyWard w WHERE w.districtCode = :districtCode AND (" +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.shortName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.key) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(w.id AS string) = :keyword)")
    Page<LegacyWard> searchByDistrictAndKeyword(
            @Param("districtCode") String districtCode,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<LegacyWard> findById(Integer id);

    Optional<LegacyWard> findByCode(String code);

    List<LegacyWard> findByDistrictCode(String districtCode);

    List<LegacyWard> findByProvinceCode(String provinceCode);
}