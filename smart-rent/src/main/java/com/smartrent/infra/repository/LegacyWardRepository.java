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

    @Query("SELECT w FROM LegacyWard w WHERE w.district.id = :districtId")
    Page<LegacyWard> findByDistrictId(@Param("districtId") Integer districtId, Pageable pageable);

    @Query("SELECT w FROM LegacyWard w WHERE w.district.id = :districtId AND (" +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(w.id AS string) = :keyword)")
    Page<LegacyWard> searchByDistrictAndKeyword(
            @Param("districtId") Integer districtId,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<LegacyWard> findById(Integer id);

    List<LegacyWard> findByDistrictId(Integer districtId);

    List<LegacyWard> findByProvinceId(Integer provinceId);
}