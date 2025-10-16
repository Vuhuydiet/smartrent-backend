package com.smartrent.infra.repository.entity;

import com.smartrent.enums.PackageLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "membership_packages")
@Table(name = "membership_packages",
        indexes = {
                @Index(name = "idx_package_level", columnList = "package_level"),
                @Index(name = "idx_is_active", columnList = "is_active"),
                @Index(name = "idx_package_code", columnList = "package_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_package_code", columnNames = "package_code")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MembershipPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id")
    Long membershipId;

    @Column(name = "package_code", nullable = false, unique = true, length = 50)
    String packageCode;

    @Column(name = "package_name", nullable = false, length = 100)
    String packageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_level", nullable = false)
    PackageLevel packageLevel;

    @Builder.Default
    @Column(name = "duration_months", nullable = false)
    Integer durationMonths = 1;

    @Column(name = "original_price", nullable = false, precision = 15, scale = 0)
    BigDecimal originalPrice;

    @Column(name = "sale_price", nullable = false, precision = 15, scale = 0)
    BigDecimal salePrice;

    @Builder.Default
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    BigDecimal discountPercentage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @OneToMany(mappedBy = "membershipPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<MembershipPackageBenefit> benefits;

    @OneToMany(mappedBy = "membershipPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<UserMembership> userMemberships;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Helper methods
    public BigDecimal calculateDiscount() {
        return originalPrice.subtract(salePrice);
    }

    public boolean isBasic() {
        return packageLevel == PackageLevel.BASIC;
    }

    public boolean isStandard() {
        return packageLevel == PackageLevel.STANDARD;
    }

    public boolean isAdvanced() {
        return packageLevel == PackageLevel.ADVANCED;
    }
}

