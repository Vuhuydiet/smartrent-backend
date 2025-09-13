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
import java.util.List;

@Entity(name = "new_provinces")
@Table(
        name = "new_provinces",
        indexes = {
                @Index(name = "idx_new_provinces_name", columnList = "name"),
                @Index(name = "idx_new_provinces_is_active", columnList = "is_active"),
                @Index(name = "idx_effective_period", columnList = "effective_from, effective_to")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_new_province_code", columnNames = {"code"})
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewProvince {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer newProvinceId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 10)
    String code;

    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "effective_from", nullable = false)
    LocalDate effectiveFrom;

    @Column(name = "effective_to")
    LocalDate effectiveTo;

    @OneToMany(mappedBy = "newProvince", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Province> mergedProvinces;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
