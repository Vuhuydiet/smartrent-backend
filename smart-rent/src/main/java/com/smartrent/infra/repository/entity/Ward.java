package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * New ward structure (effective from July 1, 2025)
 * Directly linked to province (no district level)
 * Uses VARCHAR code as primary key
 */
@Entity
@Table(name = "wards",
        indexes = {
                @Index(name = "idx_wards_province", columnList = "province_code"),
                @Index(name = "idx_wards_unit", columnList = "administrative_unit_id"),
                @Index(name = "idx_wards_name", columnList = "name")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ward {

    @Id
    @Column(length = 20)
    String code;

    @Column(nullable = false)
    String name;

    @Column(name = "name_en")
    String nameEn;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "full_name_en")
    String fullNameEn;

    @Column(name = "code_name")
    String codeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code")
    Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrative_unit_id")
    AdministrativeUnit administrativeUnit;
}