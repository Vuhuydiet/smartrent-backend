package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.District;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegacyDistrictRepository extends JpaRepository<District, Integer> {

    @Query("SELECT d FROM District d WHERE d.province.id = :provinceId")
    Page<District> findByProvinceId(@Param("provinceId") Integer provinceId, Pageable pageable);

    @Query("SELECT d FROM District d WHERE d.province.id = :provinceId AND (" +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(d.id AS string) = :keyword)")
    Page<District> searchByProvinceAndKeyword(
            @Param("provinceId") Integer provinceId,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<District> findById(Integer id);

    List<District> findByProvinceId(Integer provinceId);
}