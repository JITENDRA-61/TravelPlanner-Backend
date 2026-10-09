package com.example.travelplanner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelplanner.entity.Destination;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
    Optional<Destination> findByOsmTypeAndOsmId(String osmType, String osmId);
    boolean existsByOsmTypeAndOsmId(String osmType, String osmId);
}
