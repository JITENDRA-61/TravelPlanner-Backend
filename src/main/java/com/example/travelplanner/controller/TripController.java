package com.example.travelplanner.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.travelplanner.dto.AddDestinationToTripRequest;
import com.example.travelplanner.dto.TripDestinationDetailDto;
import com.example.travelplanner.entity.Destination;
import com.example.travelplanner.entity.Trip;
import com.example.travelplanner.entity.TripDestination;
import com.example.travelplanner.repository.DestinationRepository;
import com.example.travelplanner.repository.TripDestinationRepository;
import com.example.travelplanner.repository.TripRepository;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin
public class TripController {

    private final TripRepository tripRepository;
    private final DestinationRepository destinationRepository;
    private final TripDestinationRepository tripDestinationRepository;

    public TripController(TripRepository tripRepository,
                          DestinationRepository destinationRepository,
                          TripDestinationRepository tripDestinationRepository) {
        this.tripRepository = tripRepository;
        this.destinationRepository = destinationRepository;
        this.tripDestinationRepository = tripDestinationRepository;
    }

    @PostMapping
    public Trip createTrip(@RequestBody Trip trip) {
        return tripRepository.save(trip);
    }

    @GetMapping
    public List<Trip> getTrips() {
        return tripRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable Long id) {
        return tripRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public String deleteTrip(@PathVariable Long id) {
        tripDestinationRepository.deleteByTripId(id);
        tripRepository.deleteById(id);
        return "Trip deleted successfully";
    }

    @GetMapping("/{tripId}/destinations")
    public ResponseEntity<List<TripDestinationDetailDto>> getTripDestinations(@PathVariable Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            return ResponseEntity.notFound().build();
        }

        List<TripDestination> links = tripDestinationRepository.findByTripId(tripId);
        if (links.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<TripDestinationDetailDto> results = new ArrayList<>();
        for (TripDestination td : links) {
            Destination dest = destinationRepository.findById(td.getDestinationId()).orElse(null);
            results.add(new TripDestinationDetailDto(
                td.getId(),
                td.getTripId(),
                td.getDestinationId(),
                td.getVisitDate(),
                td.getNotes(),
                dest
            ));
        }

        return ResponseEntity.ok(results);
    }

    @PostMapping("/{tripId}/destinations")
    public ResponseEntity<Map<String, Object>> addDestinationToTrip(
            @PathVariable Long tripId,
            @RequestBody AddDestinationToTripRequest request) {

        Map<String, Object> response = new HashMap<>();

        if (!tripRepository.existsById(tripId)) {
            response.put("success", false);
            response.put("message", "Trip not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Determine destination entity
        Destination destination = null;
        if (request.getDestinationId() != null) {
            destination = destinationRepository.findById(request.getDestinationId()).orElse(null);
        } else if (request.getOsmType() != null && request.getOsmId() != null) {
            // Find existing by osmType and osmId to prevent duplicate entries
            destination = destinationRepository.findByOsmTypeAndOsmId(
                request.getOsmType(), request.getOsmId()
            ).orElse(null);
        }

        // If not found in database, save the newly discovered OSM destination to MySQL
        if (destination == null) {
            Destination newDest = new Destination(
                request.getName(),
                request.getCity(),
                request.getState(),
                request.getCountry(),
                request.getDescription(),
                request.getCategory(),
                request.getLatitude(),
                request.getLongitude(),
                request.getOsmType(),
                request.getOsmId()
            );
            if (request.getVisitDate() != null) {
                newDest.setVisitDate(request.getVisitDate());
            }
            destination = destinationRepository.save(newDest);
        }

        // Check if destination is already associated with this trip
        if (tripDestinationRepository.existsByTripIdAndDestinationId(tripId, destination.getId())) {
            response.put("success", false);
            response.put("alreadyExists", true);
            response.put("message", "This destination is already in this trip.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        // Associate destination with trip
        TripDestination tripDestination = new TripDestination(
            tripId,
            destination.getId(),
            request.getVisitDate(),
            request.getNotes()
        );
        TripDestination saved = tripDestinationRepository.save(tripDestination);

        response.put("success", true);
        response.put("message", "Destination added to your trip.");
        response.put("tripDestinationId", saved.getId());
        response.put("destination", destination);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{tripId}/destinations/{destinationId}")
    public ResponseEntity<Map<String, Object>> removeDestinationFromTrip(
            @PathVariable Long tripId,
            @PathVariable Long destinationId) {

        Map<String, Object> response = new HashMap<>();
        Optional<TripDestination> link = tripDestinationRepository.findByTripIdAndDestinationId(tripId, destinationId);

        if (link.isPresent()) {
            tripDestinationRepository.delete(link.get());
            response.put("success", true);
            response.put("message", "Destination removed from trip successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Destination was not found in this trip.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}