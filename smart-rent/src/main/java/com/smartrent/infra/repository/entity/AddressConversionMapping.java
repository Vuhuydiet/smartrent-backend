package com.smartrent.infra.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing the mapping between old and new administrative structures.
 * <p>
 * This table tracks how addresses in the old structure (before July 1, 2025)
 * map to the new structure (after July 1, 2025).
 * </p>
 *
 * <p>Old structure: Province → District → Ward</p>
 * <p>New structure: Province → Ward (no districts)</p>
 */
@Entity(name = "address_conversion_mappings")
@Table(name = "address_conversion_mappings",
        indexes = {
                @Index(name = "idx_old_address", columnList = "old_province_id, old_district_id, old_ward_id"),
                @Index(name = "idx_new_address", columnList = "new_province_id, new_ward_id"),
                @Index(name = "idx_old_province", columnList = "old_province_id"),
                @Index(name = "idx_old_district", columnList = "old_district_id"),
                @Index(name = "idx_old_ward", columnList = "old_ward_id"),
                @Index(name = "idx_new_province", columnList = "new_province_id"),
                @Index(name = "idx_new_ward", columnList = "new_ward_id")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressConversionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long mappingId;

    // Old structure (before July 1, 2025)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_province_id")
    Province oldProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_district_id")
    District oldDistrict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_ward_id")
    Ward oldWard;

    // New structure (after July 1, 2025)
    // Note: No district in new structure - wards directly under provinces
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_province_id")
    Province newProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_ward_id")
    Ward newWard;

    @Column(name = "conversion_note", columnDefinition = "TEXT")
    String conversionNote;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "conversion_accuracy")
    Integer conversionAccuracy; // Percentage (0-100) indicating accuracy of conversion

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    /**
     * Get formatted old address
     */
    public String getOldAddressFormatted() {
        if (oldWard == null || oldDistrict == null || oldProvince == null) {
            return null;
        }
        return String.format("%s, %s, %s",
                oldWard.getName(),
                oldDistrict.getName(),
                oldProvince.getName());
    }

    /**
     * Get formatted new address
     */
    public String getNewAddressFormatted() {
        if (newWard == null || newProvince == null) {
            return null;
        }
        return String.format("%s, %s",
                newWard.getName(),
                newProvince.getName());
    }

    /**
     * Check if this is a complete mapping with all required fields
     */
    public boolean isCompleteMapping() {
        return oldProvince != null && oldDistrict != null && oldWard != null
                && newProvince != null && newWard != null;
    }
}
