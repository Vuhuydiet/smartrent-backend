package com.smartrent.infra.repository;

import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.infra.repository.entity.Listing;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class ListingSpecification {
    public static Specification<Listing> filter(ListingFilterRequest filter) {
        return (Root<Listing> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getAddressId() != null) {
                predicates.add(cb.equal(root.get("addressId"), filter.getAddressId()));
            }
            // Name-based address filtering using subqueries to avoid changing entity mapping
            if (filter.getProvinceId() != null || filter.getProvinceName() != null
                    || filter.getDistrictId() != null || filter.getDistrictName() != null
                    || filter.getWardName() != null
                    || filter.getStreetId() != null || filter.getStreetName() != null
                    || filter.getAddressText() != null) {

                // Subquery selects addressId from Address matching the name/ids
                Subquery<Long> addressSub = query.subquery(Long.class);
                Root<com.smartrent.infra.repository.entity.Address> addr = addressSub.from(com.smartrent.infra.repository.entity.Address.class);
                addressSub.select(addr.get("addressId"));

                List<Predicate> addrPreds = new ArrayList<>();

                if (filter.getProvinceId() != null) {
                    addrPreds.add(cb.equal(addr.get("province").get("provinceId"), filter.getProvinceId()));
                }
                if (filter.getProvinceName() != null && !filter.getProvinceName().isBlank()) {
                    addrPreds.add(cb.like(cb.lower(addr.get("province").get("name")), "%" + filter.getProvinceName().toLowerCase() + "%"));
                }
                if (filter.getDistrictId() != null) {
                    addrPreds.add(cb.equal(addr.get("district").get("districtId"), filter.getDistrictId()));
                }
                if (filter.getDistrictName() != null && !filter.getDistrictName().isBlank()) {
                    addrPreds.add(cb.like(cb.lower(addr.get("district").get("name")), "%" + filter.getDistrictName().toLowerCase() + "%"));
                }
                if (filter.getWardName() != null && !filter.getWardName().isBlank()) {
                    addrPreds.add(cb.like(cb.lower(addr.get("ward").get("name")), "%" + filter.getWardName().toLowerCase() + "%"));
                }
                if (filter.getStreetId() != null) {
                    addrPreds.add(cb.equal(addr.get("street").get("streetId"), filter.getStreetId()));
                }
                if (filter.getStreetName() != null && !filter.getStreetName().isBlank()) {
                    addrPreds.add(cb.like(cb.lower(addr.get("street").get("name")), "%" + filter.getStreetName().toLowerCase() + "%"));
                }
                if (filter.getAddressText() != null && !filter.getAddressText().isBlank()) {
                    addrPreds.add(cb.like(cb.lower(addr.get("fullAddress")), "%" + filter.getAddressText().toLowerCase() + "%"));
                }

                addressSub.where(cb.and(addrPreds.toArray(new Predicate[0])));
                predicates.add(root.get("addressId").in(addressSub));
            }
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), filter.getCategoryId()));
            }
            if (filter.getPriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getPriceMin()));
            }
            if (filter.getPriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getPriceMax()));
            }
            if (filter.getAreaMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), filter.getAreaMin()));
            }
            if (filter.getAreaMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), filter.getAreaMax()));
            }
            if (filter.getCreatedAt() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("postDate"), filter.getCreatedAt().atStartOfDay()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getVerified() != null) {
                predicates.add(cb.equal(root.get("verified"), filter.getVerified()));
            }
            if (filter.getBedrooms() != null) {
                predicates.add(cb.equal(root.get("bedrooms"), filter.getBedrooms()));
            }
            if (filter.getDirection() != null) {
                predicates.add(cb.equal(root.get("direction"), filter.getDirection()));
            }
            // Amenities filter
            if (filter.getAmenities() != null && !filter.getAmenities().isEmpty()) {
                Join<Object, Object> amenityJoin = root.join("amenities", JoinType.INNER);
                predicates.add(amenityJoin.get("amenityId").in(filter.getAmenities()));
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
