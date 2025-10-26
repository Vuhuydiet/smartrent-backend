package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Legacy ward structure (before July 1, 2025)
 * Uses integer ID as primary key
 */
@Entity
@Table(name = "ward")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyWard {

    @Id
    Integer id;

    @Column(name = "_name", length = 50, nullable = false)
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

    @OneToMany(mappedBy = "ward", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Street> streets;
}