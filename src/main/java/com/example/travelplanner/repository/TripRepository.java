package com.example.travelplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelplanner.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {
}