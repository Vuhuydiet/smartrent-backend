package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "amenities")
@Table(name = "amenities",
        indexes = {
                @Index(name = "idx_name", columnList = "name"),
                @Index(name = "idx_category", columnList = "category"),
                @Index(name = "idx_is_active", columnList = "is_active")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long amenityId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 50)
    String icon;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AmenityCategory category;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @ManyToMany(mappedBy = "amenities")
    List<Listing> listings;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    public enum AmenityCategory {
        BASIC, CONVENIENCE, SECURITY, ENTERTAINMENT, TRANSPORT
    }
}
