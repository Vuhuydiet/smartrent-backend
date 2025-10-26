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
            "LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "CAST(p.id AS string) = :keyword")
    Page<LegacyProvince> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<LegacyProvince> findById(Integer id);

    List<LegacyProvince> findByNameContainingIgnoreCase(String name);
}