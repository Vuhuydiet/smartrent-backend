package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Address entity - stores formatted address strings for both old and new structures
 * Matches database schema from V7 migration
 */
@Entity(name = "addresses")
@Table(name = "addresses")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    Long addressId;

    /**
     * Formatted full address for old structure (63 provinces, 3-tier)
     * Example: "123 Đường Nguyễn Trãi, Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội"
     */
    @Column(name = "full_address", columnDefinition = "TEXT")
    String fullAddress;

    /**
     * Formatted full address for new structure (34 provinces, 2-tier)
     * Example: "123 Đường Nguyễn Trãi, Phường Ba Đình, Thành phố Hà Nội"
     */
    @Column(name = "full_newaddress", columnDefinition = "TEXT")
    String fullNewAddress;

    @Column(name = "latitude", precision = 10, scale = 8)
    BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    BigDecimal longitude;

    @OneToMany(mappedBy = "address", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Listing> listings;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    /**
     * Get the appropriate address display based on which format is available
     */
    public String getDisplayAddress() {
        if (fullNewAddress != null && !fullNewAddress.isEmpty()) {
            return fullNewAddress;
        }
        return fullAddress;
    }

    /**
     * Check if this address uses the new structure
     */
    public boolean isNewStructure() {
        return fullNewAddress != null && !fullNewAddress.isEmpty();
    }
}
