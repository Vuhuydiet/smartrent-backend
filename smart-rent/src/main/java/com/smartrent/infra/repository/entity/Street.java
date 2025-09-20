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

@Entity(name = "streets")
@Table(name = "streets",
        indexes = {
                @Index(name = "idx_ward_id", columnList = "ward_id"),
                @Index(name = "idx_name", columnList = "name"),
                @Index(name = "idx_is_active", columnList = "is_active")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Street {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long streetId;

    @Column(nullable = false, length = 200)
    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    Ward ward;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @OneToMany(mappedBy = "street", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Address> addresses;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

}