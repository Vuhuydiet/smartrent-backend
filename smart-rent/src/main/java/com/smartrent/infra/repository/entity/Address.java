package com.smartrent.infra.repository.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "addresses")
@Table(name = "addresses",
        indexes = {
                @Index(name = "idx_street_id", columnList = "street_id"),
                @Index(name = "idx_ward_id", columnList = "ward_id"),
                @Index(name = "idx_district_id", columnList = "district_id"),
                @Index(name = "idx_province_id", columnList = "province_id"),
                @Index(name = "idx_coordinates", columnList = "latitude, longitude"),
                @Index(name = "idx_is_verified", columnList = "is_verified")
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

    @Column(name = "street_number", length = 20)
    String streetNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "street_id", nullable = false)
    Street street;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    Ward ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    Province province;

    @Column(name = "full_address", columnDefinition = "TEXT")
    String fullAddress;

    @Column(name = "latitude", precision = 10, scale = 8)
    BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    BigDecimal longitude;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    Boolean isVerified = false;

    @OneToMany(mappedBy = "address", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Listing> listings;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Helper method to get full address display
    public String getFullAddressDisplay() {
        StringBuilder sb = new StringBuilder();
        if (streetNumber != null) {
            sb.append(streetNumber).append(" ");
        }
        sb.append(street.getName()).append(", ");
        sb.append(ward.getName()).append(", ");
        sb.append(district.getName()).append(", ");
        sb.append(province.getDisplayName()); // Use display name for merged provinces
        return sb.toString();
    }
}
