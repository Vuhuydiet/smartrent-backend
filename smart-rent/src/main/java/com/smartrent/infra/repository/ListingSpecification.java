package com.smartrent.infra.repository;

import com.smartrent.controller.dto.request.ListingFilterRequest;
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
            // Nếu muốn filter theo provinceId, districtId, streetId thì cần truyền vào danh sách addressId phù hợp từ service/controller
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("categoryId"), filter.getCategory()));
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
            // Amenities filter (join với bảng amenities)
            if (filter.getAmenities() != null && !filter.getAmenities().isEmpty()) {
                Join<Object, Object> amenityJoin = root.join("amenities", JoinType.INNER);
                predicates.add(amenityJoin.get("amenityId").in(filter.getAmenities()));
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
