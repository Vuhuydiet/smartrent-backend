package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.LegacyProvince;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegacyProvinceRepository extends JpaRepository<LegacyProvince, Integer> {

    @Query("SELECT p FROM LegacyProvince p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.shortName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.key) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(p.id AS string) = :keyword")
    Page<LegacyProvince> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<LegacyProvince> findById(Integer id);

    Optional<LegacyProvince> findByCode(String code);

    List<LegacyProvince> findByCodeIn(List<String> codes);

    List<LegacyProvince> findByNameContainingIgnoreCase(String name);

    /**
     * Suggestion-tier match against the province itself.
     *
     * <p>The seed data left {@code province_key} in a space-stripped form
     * (e.g. {@code "thanhphohochiminh"}), so an input like
     * {@code "ho chi minh"} (TextNormalizer output, with spaces) cannot LIKE
     * against {@code key}. We accept two pre-computed forms from the caller
     * — {@code normalized} (with spaces, used against {@code name}/{@code shortName}
     * via the DB's accent-insensitive collation) and {@code compactKey}
     * (no spaces, used against the legacy {@code key} column) — and OR them
     * so either layout matches.
     */
    @Query("""
        SELECT p FROM LegacyProvince p
        WHERE LOWER(p.name)      LIKE LOWER(CONCAT('%', :normalized, '%'))
           OR LOWER(p.shortName) LIKE LOWER(CONCAT('%', :normalized, '%'))
           OR p.key              LIKE CONCAT('%', :compactKey, '%')
        ORDER BY p.name ASC
    """)
    List<LegacyProvince> findSuggestionMatches(
            @Param("normalized") String normalized,
            @Param("compactKey") String compactKey,
            org.springframework.data.domain.Pageable pageable);
}