package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Legacy province structure (before July 1, 2025)
 * Uses integer ID as primary key
 */
@Entity
@Table(name = "province")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyProvince {

    @Id
    Integer id;

    @Column(name = "_name", length = 50)
    String name;

    @Column(name = "name_en", length = 100)
    String nameEn;

    @Column(name = "_code", length = 20)
    String code;

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<District> districts;
}