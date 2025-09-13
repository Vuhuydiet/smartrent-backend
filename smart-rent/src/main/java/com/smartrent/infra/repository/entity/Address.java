package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity(name = "addresses")
@Table(name = "addresses",
        indexes = {
                @Index(name = "idx_ward_id", columnList = "ward_id"),
                @Index(name = "idx_new_ward_id", columnList = "new_ward_id"),
                @Index(name = "idx_district_id", columnList = "district_id"),
                @Index(name = "idx_province_id", columnList = "province_id"),
                @Index(name = "idx_street_id", columnList = "street_id"),
                @Index(name = "idx_coordinates", columnList = "latitude, longitude"),
                @Index(name = "idx_original_location", columnList = "original_ward_id, original_district_id, original_province_id")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long addressId;

    @Column(name = "full_address", nullable = false, columnDefinition = "TEXT")
    String fullAddress;

    @Column(name = "house_number", length = 20)
    String houseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "street_id")
    Street street;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    Ward ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_ward_id", nullable = false)
    NewWard newWard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    Province province;

    // Original address before merging (if any)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_ward_id")
    Ward originalWard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_district_id")
    District originalDistrict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_province_id")
    Province originalProvince;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}