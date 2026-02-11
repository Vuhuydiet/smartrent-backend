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

    @Column(name = "_province_id")
    Integer provinceId;

    @Column(name = "_district_id")
    Integer districtId;
}