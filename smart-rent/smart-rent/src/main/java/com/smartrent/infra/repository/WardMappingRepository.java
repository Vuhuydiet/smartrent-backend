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

    /**
     * Find the default/primary mapping for a legacy ward
     * When a legacy ward splits to multiple new wards, returns the primary destination
     */
    @Query("SELECT wm FROM WardMapping wm WHERE wm.legacyWard.id = :legacyId AND wm.isDefaultNewWard = true")
    Optional<WardMapping> findDefaultByLegacyWardId(@Param("legacyId") Integer legacyId);

    /**
     * Find the default/primary legacy ward for a new ward
     * When multiple legacy wards merge into one new ward, returns the primary source
     */
    @Query("SELECT wm FROM WardMapping wm WHERE wm.newWard.code = :newCode AND wm.isDefaultNewWard = true")
    Optional<WardMapping> findDefaultByNewWardCode(@Param("newCode") String newCode);
}