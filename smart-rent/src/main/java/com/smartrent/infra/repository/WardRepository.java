package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Ward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, Integer> {

    @Query("SELECT w FROM Ward w WHERE w.provinceCode = :provinceCode")
    Page<Ward> findByProvinceCode(@Param("provinceCode") String provinceCode, Pageable pageable);

    @Query("SELECT w FROM Ward w WHERE w.provinceCode = :provinceCode AND (" +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.shortName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.key) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Ward> searchByProvinceAndKeyword(
            @Param("provinceCode") String provinceCode,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<Ward> findByCode(String code);

    // Find all wards by province code (without pagination)
    @Query("SELECT w FROM Ward w WHERE w.provinceCode = :provinceCode ORDER BY w.name")
    List<Ward> findAllByProvinceCode(@Param("provinceCode") String provinceCode);

    // Search wards by keyword across all provinces
    @Query("SELECT w FROM Ward w WHERE " +
            "LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.shortName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.key) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY w.name")
    Page<Ward> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}