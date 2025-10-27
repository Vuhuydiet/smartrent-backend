package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    List<Project> findByProvinceId(Integer provinceId);

    List<Project> findByDistrictId(Integer districtId);

    @Query("SELECT p FROM Project p WHERE p.provinceId = :provinceId AND p.districtId = :districtId")
    List<Project> findByProvinceIdAndDistrictId(@Param("provinceId") Integer provinceId,
                                                  @Param("districtId") Integer districtId);

    @Query("SELECT p FROM Project p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Project> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<Project> findById(Integer id);
}
