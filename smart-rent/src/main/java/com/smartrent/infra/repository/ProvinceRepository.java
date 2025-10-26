package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Province;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, String> {

    // Find province by code
    Optional<Province> findByCode(String code);

    // Search provinces by keyword (name, nameEn, or code)
    @Query("SELECT p FROM Province p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY p.name")
    Page<Province> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

}