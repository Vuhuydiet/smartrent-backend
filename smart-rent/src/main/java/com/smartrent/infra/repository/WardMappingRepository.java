package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.WardMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardMappingRepository extends JpaRepository<WardMapping, Integer> {

    @Query("SELECT wm FROM WardMapping wm WHERE wm.legacyWard.id = :legacyId")
    List<WardMapping> findByLegacyWardId(@Param("legacyId") Integer legacyId);

    @Query("SELECT wm FROM WardMapping wm WHERE wm.newWard.code = :newCode")
    List<WardMapping> findByNewWardCode(@Param("newCode") String newCode);
}