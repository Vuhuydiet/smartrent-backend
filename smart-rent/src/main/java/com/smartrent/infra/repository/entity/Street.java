package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Legacy street structure (before July 1, 2025)
 * Uses integer ID as primary key
 */
@Entity
@Table(name = "street")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Street {

    @Id
    Integer id;

    @Column(name = "_name", length = 100)
    String name;

    @Column(name = "name_en", length = 100)
    String nameEn;

    @Column(name = "_prefix", length = 20)
    String prefix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "_province_id")
    LegacyProvince province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "_district_id")
    District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "_ward_id")
    LegacyWard ward;
}