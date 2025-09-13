package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "amenities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_amenity_name", columnNames = "name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Amenity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long amenityId;

    @Column(nullable = false, length = 100)
    String name;

    @Lob
    String description;
}
