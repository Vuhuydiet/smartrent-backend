package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Street;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreetRepository extends JpaRepository<Street, Integer> {

    List<Street> findByProvinceId(Integer provinceId);

    List<Street> findByDistrictId(Integer districtId);

    @Query("SELECT s FROM Street s WHERE s.provinceId = :provinceId AND s.districtId = :districtId")
    List<Street> findByProvinceIdAndDistrictId(@Param("provinceId") Integer provinceId,
                                                 @Param("districtId") Integer districtId);

    @Query("SELECT s FROM Street s WHERE " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Street> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<Street> findById(Integer id);
}