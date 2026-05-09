package com.smartrent.infra.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Directed follow edge: {@code followerId} subscribes to {@code followingId}'s activity
 * (currently: new listings). Edges are unique per (follower, following) pair so the
 * repository can rely on insert-or-noop semantics for follow, and a single delete for unfollow.
 */
@Entity(name = "user_follows")
@Table(
        name = "user_follows",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_follows_follower_following",
                        columnNames = {"follower_id", "following_id"})
        },
        indexes = {
                @Index(name = "idx_user_follows_following", columnList = "following_id"),
                @Index(name = "idx_user_follows_follower", columnList = "follower_id")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "follower_id", nullable = false, length = 36)
    String followerId;

    @Column(name = "following_id", nullable = false, length = 36)
    String followingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
