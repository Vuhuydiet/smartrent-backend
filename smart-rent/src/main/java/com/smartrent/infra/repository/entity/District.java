package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Legacy district structure (before July 1, 2025)
 * Uses integer ID as primary key
 */
@Entity
@Table(name = "district")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class District {

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

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<LegacyWard> wards;
}