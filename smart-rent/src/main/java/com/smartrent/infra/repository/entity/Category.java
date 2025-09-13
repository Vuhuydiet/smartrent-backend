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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity(name = "categories")
@Table(name = "categories",
        indexes = {
                @Index(name = "idx_slug", columnList = "slug", unique = true),
                @Index(name = "idx_name", columnList = "name"),
                @Index(name = "idx_is_active", columnList = "is_active")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long categoryId;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "slug", unique = true, length = 120)
    String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "icon", length = 50)
    String icon;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @OneToMany(mappedBy = "categoryId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Listing> listings;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
