package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Administrative unit types (e.g., Municipality, Province, Ward, Commune)
 * Reference table for the new address structure
 */
@Entity
@Table(name = "administrative_units")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdministrativeUnit {

    @Id
    Integer id;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "full_name_en")
    String fullNameEn;

    @Column(name = "short_name")
    String shortName;

    @Column(name = "short_name_en")
    String shortNameEn;

    @Column(name = "code_name")
    String codeName;

    @Column(name = "code_name_en")
    String codeNameEn;
}