package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "provinces",
        indexes = {
                @Index(name = "idx_provinces_unit", columnList = "administrative_unit_id"),
                @Index(name = "idx_provinces_name", columnList = "name")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Province {

    @Id
    @Column(length = 20)
    String code;

    @Column(nullable = false)
    String name;

    @Column(name = "name_en")
    String nameEn;

    @Column(name = "full_name", nullable = false)
    String fullName;

    @Column(name = "full_name_en")
    String fullNameEn;

    @Column(name = "code_name")
    String codeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrative_unit_id")
    AdministrativeUnit administrativeUnit;

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Ward> wards;
}