package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}

