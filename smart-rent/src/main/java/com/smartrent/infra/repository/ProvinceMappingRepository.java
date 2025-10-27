package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.ProvinceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProvinceMappingRepository extends JpaRepository<ProvinceMapping, Integer> {

    @Query("SELECT pm FROM ProvinceMapping pm WHERE pm.legacyProvince.id = :legacyId")
    Optional<ProvinceMapping> findByLegacyProvinceId(@Param("legacyId") Integer legacyId);

    @Query("SELECT pm FROM ProvinceMapping pm WHERE pm.newProvince.code = :newCode")
    Optional<ProvinceMapping> findByNewProvinceCode(@Param("newCode") String newCode);
}