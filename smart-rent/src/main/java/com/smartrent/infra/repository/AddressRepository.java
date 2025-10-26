package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Get addresses by street
//    List<Address> findByStreetStreetIdOrderByStreetNumber(Street street);

    // Search addresses by coordinates (nearby addresses)
    @Query("SELECT a FROM addresses a WHERE " +
           "ABS(a.latitude - :latitude) <= :latDelta AND " +
           "ABS(a.longitude - :longitude) <= :lngDelta " +
           "ORDER BY " +
           "SQRT(POWER(a.latitude - :latitude, 2) + POWER(a.longitude - :longitude, 2))")
    List<Address> findNearbyAddresses(@Param("latitude") BigDecimal latitude,
                                     @Param("longitude") BigDecimal longitude,
                                     @Param("latDelta") BigDecimal latDelta,
                                     @Param("lngDelta") BigDecimal lngDelta);

    // Find by full address text search
    List<Address> findByFullAddressContainingIgnoreCaseOrderByFullAddress(String searchTerm);

    // Get verified addresses only
    List<Address> findByIsVerifiedTrueOrderByFullAddress();

////    // Find exact address match
//    Optional<Address> findByStreetNumberAndStreetStreetIdAndWardWardIdAndDistrictDistrictIdAndProvinceProvinceId(
//            Street street, Ward ward, District district, Province province);
}
