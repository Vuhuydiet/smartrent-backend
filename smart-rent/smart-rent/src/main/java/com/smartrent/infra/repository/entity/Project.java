package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Project entity for legacy address structure
 * Contains project/location information
 */
@Entity
@Table(name = "project")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Project {

    @Id
    Integer id;

    @Column(name = "_name", length = 200)
    String name;

    @Column(name = "name_en", length = 100)
    String nameEn;

    @Column(name = "_province_id")
    Integer provinceId;

    @Column(name = "_district_id")
    Integer districtId;

    @Column(name = "_lat")
    Double latitude;

    @Column(name = "_lng")
    Double longitude;
}