package com.smartrent.infra.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity(name = "listing_click_daily")
@Table(name = "listing_click_daily",
        uniqueConstraints = @UniqueConstraint(columnNames = {"listing_id", "click_date"}),
        indexes = {
                @Index(name = "idx_listing_id", columnList = "listing_id"),
                @Index(name = "idx_click_date", columnList = "click_date")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingClickDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "listing_id", nullable = false)
    Long listingId;

    @Column(name = "click_date", nullable = false)
    LocalDate clickDate;

    @Builder.Default
    @Column(name = "click_count", nullable = false)
    Integer clickCount = 0;
}
