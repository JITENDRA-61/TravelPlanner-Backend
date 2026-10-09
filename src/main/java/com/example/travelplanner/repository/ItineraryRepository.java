package com.example.travelplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelplanner.entity.Itinerary;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
}