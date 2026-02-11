package com.smartrent.infra.repository;

import com.smartrent.enums.PackageLevel;
import com.smartrent.infra.repository.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Long> {

    Optional<MembershipPackage> findByPackageCode(String packageCode);

    List<MembershipPackage> findByIsActiveTrueOrderByPackageLevelAsc();

    List<MembershipPackage> findByPackageLevel(PackageLevel packageLevel);

    List<MembershipPackage> findByPackageLevelAndIsActiveTrue(PackageLevel packageLevel);

    @Query("SELECT mp FROM membership_packages mp WHERE mp.isActive = true AND mp.packageLevel = :level ORDER BY mp.salePrice ASC")
    List<MembershipPackage> findActivePackagesByLevel(PackageLevel level);

    boolean existsByPackageCode(String packageCode);
}

