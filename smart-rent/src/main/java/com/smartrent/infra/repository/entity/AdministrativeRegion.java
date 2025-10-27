package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Administrative regions of Vietnam (e.g., Northeast, Northwest, Red River Delta)
 * Reference table for the new address structure
 */
@Entity
@Table(name = "administrative_regions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdministrativeRegion {

    @Id
    Integer id;

    @Column(nullable = false)
    String name;

    @Column(name = "name_en", nullable = false)
    String nameEn;

    @Column(name = "code_name")
    String codeName;

    @Column(name = "code_name_en")
    String codeNameEn;
}