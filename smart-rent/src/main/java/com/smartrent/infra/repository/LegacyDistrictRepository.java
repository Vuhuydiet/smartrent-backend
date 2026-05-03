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

    @Query("SELECT d FROM District d WHERE d.provinceCode = :provinceCode")
    Page<District> findByProvinceCode(@Param("provinceCode") String provinceCode, Pageable pageable);

    @Query("SELECT d FROM District d WHERE d.provinceCode = :provinceCode AND (" +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.shortName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.key) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(d.id AS string) = :keyword)")
    Page<District> searchByProvinceAndKeyword(
            @Param("provinceCode") String provinceCode,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<District> findById(Integer id);

    Optional<District> findByCode(String code);

    List<District> findByProvinceCode(String provinceCode);

    /**
     * Suggestion-tier match against the district itself.
     *
     * <p>Mirrors {@code LegacyProvinceRepository.findSuggestionMatches} —
     * accepts {@code normalized} (with spaces, matched against name/shortName
     * via accent-insensitive collation) and {@code compactKey} (no spaces,
     * matched against the legacy {@code key} column whose seed data dropped
     * spaces, e.g. {@code "quan1"}).
     *
     * <p>Only filters by the district's own columns — does NOT match on the
     * denormalized {@code province_name} so that a province-level query does
     * not pull every district in that province as a separate suggestion.
     */
    @Query("""
        SELECT d FROM District d
        LEFT JOIN FETCH d.province
        WHERE LOWER(d.name)      LIKE LOWER(CONCAT('%', :normalized, '%'))
           OR LOWER(d.shortName) LIKE LOWER(CONCAT('%', :normalized, '%'))
           OR d.key              LIKE CONCAT('%', :compactKey, '%')
        ORDER BY d.provinceName ASC, d.name ASC
    """)
    List<District> findSuggestionMatches(
            @Param("normalized") String normalized,
            @Param("compactKey") String compactKey,
            Pageable pageable);

    @Query("""
        SELECT d FROM District d
        LEFT JOIN FETCH d.province
        WHERE d.provinceCode = :provinceCode
          AND ( LOWER(d.name)      LIKE LOWER(CONCAT('%', :normalized, '%'))
             OR LOWER(d.shortName) LIKE LOWER(CONCAT('%', :normalized, '%'))
             OR d.key              LIKE CONCAT('%', :compactKey, '%') )
        ORDER BY d.name ASC
    """)
    List<District> findSuggestionMatchesByProvince(
            @Param("provinceCode") String provinceCode,
            @Param("normalized") String normalized,
            @Param("compactKey") String compactKey,
            Pageable pageable);
}