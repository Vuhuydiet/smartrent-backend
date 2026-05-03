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

    /**
     * Ward-only suggestion match. Mirrors {@code findSuggestionMatches} on
     * the province / district repositories: filters by the ward's own
     * columns only, never by the denormalized district / province columns,
     * so a province- or district-level query cannot pull every ward inside
     * it as a separate suggestion.
     *
     * <p>Accepts both forms because the seed {@code ward_key} is corrupted
     * (spaces become {@code _} and several Vietnamese vowels are stripped to
     * empty, e.g. {@code "phng_phc_x"} for "Phường Phúc Xá"). The {@code name}
     * branch is what actually carries weight; the {@code key} branch is kept
     * for the rare row whose key happens to be in compact form.
     */
    @Query("""
        SELECT w FROM LegacyWard w
        LEFT JOIN FETCH w.province
        LEFT JOIN FETCH w.district
        WHERE LOWER(w.name)      LIKE LOWER(CONCAT('%', :normalized, '%'))
           OR LOWER(w.shortName) LIKE LOWER(CONCAT('%', :normalized, '%'))
           OR w.key              LIKE CONCAT('%', :compactKey, '%')
        ORDER BY w.provinceName ASC, w.districtName ASC, w.name ASC
    """)
    List<LegacyWard> findWardSuggestionMatches(
            @Param("normalized") String normalized,
            @Param("compactKey") String compactKey,
            Pageable pageable);

    @Query("""
        SELECT w FROM LegacyWard w
        LEFT JOIN FETCH w.province
        LEFT JOIN FETCH w.district
        WHERE w.provinceCode = :provinceCode
          AND ( LOWER(w.name)      LIKE LOWER(CONCAT('%', :normalized, '%'))
             OR LOWER(w.shortName) LIKE LOWER(CONCAT('%', :normalized, '%'))
             OR w.key              LIKE CONCAT('%', :compactKey, '%') )
        ORDER BY w.districtName ASC, w.name ASC
    """)
    List<LegacyWard> findWardSuggestionMatchesByProvince(
            @Param("provinceCode") String provinceCode,
            @Param("normalized") String normalized,
            @Param("compactKey") String compactKey,
            Pageable pageable);
}
