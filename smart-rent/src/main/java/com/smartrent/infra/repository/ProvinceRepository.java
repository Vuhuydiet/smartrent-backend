package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Province;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {

    // Find province by code
    Optional<Province> findByCode(String code);

    // Search provinces by keyword (name, shortName, key, alias, or code) - with pagination
    @Query("SELECT p FROM Province p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.shortName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.key) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY p.name")
    Page<Province> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Search provinces by keyword (name, shortName, key, alias, or code) - without pagination
    @Query("SELECT p FROM Province p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.shortName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.key) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.alias) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY p.name")
    List<Province> searchByKeyword(@Param("keyword") String keyword);

}