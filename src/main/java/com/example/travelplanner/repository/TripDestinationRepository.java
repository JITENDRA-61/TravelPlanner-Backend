package com.example.travelplanner.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.travelplanner.entity.TripDestination;

public interface TripDestinationRepository extends JpaRepository<TripDestination, Long> {
    List<TripDestination> findByTripId(Long tripId);
    Optional<TripDestination> findByTripIdAndDestinationId(Long tripId, Long destinationId);
    boolean existsByTripIdAndDestinationId(Long tripId, Long destinationId);

    @Transactional
    void deleteByTripIdAndDestinationId(Long tripId, Long destinationId);

    @Transactional
    void deleteByTripId(Long tripId);

    @Transactional
    void deleteByDestinationId(Long destinationId);
}
