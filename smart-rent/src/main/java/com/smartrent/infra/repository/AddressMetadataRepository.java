package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.AddressMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressMetadataRepository extends JpaRepository<AddressMetadata, Long> {

    Optional<AddressMetadata> findByAddress_AddressId(Long addressId);

    // Old structure queries
    List<AddressMetadata> findByProvinceId(Integer provinceId);

    List<AddressMetadata> findByDistrictId(Integer districtId);

    List<AddressMetadata> findByWardId(Integer wardId);

    @Query("SELECT am FROM AddressMetadata am WHERE am.provinceId = :provinceId AND am.districtId = :districtId")
    List<AddressMetadata> findByProvinceIdAndDistrictId(@Param("provinceId") Integer provinceId,
                                                          @Param("districtId") Integer districtId);

    @Query("SELECT am FROM AddressMetadata am WHERE am.provinceId = :provinceId AND am.districtId = :districtId AND am.wardId = :wardId")
    List<AddressMetadata> findByProvinceIdAndDistrictIdAndWardId(@Param("provinceId") Integer provinceId,
                                                                   @Param("districtId") Integer districtId,
                                                                   @Param("wardId") Integer wardId);

    // New structure queries
    List<AddressMetadata> findByNewProvinceCode(String newProvinceCode);

    List<AddressMetadata> findByNewWardCode(String newWardCode);

    @Query("SELECT am FROM AddressMetadata am WHERE am.newProvinceCode = :provinceCode AND am.newWardCode = :wardCode")
    List<AddressMetadata> findByNewProvinceCodeAndNewWardCode(@Param("provinceCode") String provinceCode,
                                                                @Param("wardCode") String wardCode);

    // Common queries
    List<AddressMetadata> findByStreetId(Integer streetId);

    List<AddressMetadata> findByProjectId(Integer projectId);

    List<AddressMetadata> findByAddressType(AddressMetadata.AddressType addressType);
}
