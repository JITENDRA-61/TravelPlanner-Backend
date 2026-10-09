package com.example.travelplanner.controller;

import java.util.List;
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

import com.example.travelplanner.entity.Destination;
import com.example.travelplanner.repository.DestinationRepository;
import com.example.travelplanner.repository.TripDestinationRepository;

@RestController
@RequestMapping("/api/destinations")
@CrossOrigin
public class DestinationController {

    private final DestinationRepository destinationRepository;
    private final TripDestinationRepository tripDestinationRepository;

    public DestinationController(DestinationRepository destinationRepository,
                                 TripDestinationRepository tripDestinationRepository) {
        this.destinationRepository = destinationRepository;
        this.tripDestinationRepository = tripDestinationRepository;
    }

    @PostMapping
    public ResponseEntity<Destination> addDestination(@RequestBody Destination destination) {
        // Prevent duplicate OSM destination entries
        if (destination.getOsmType() != null && destination.getOsmId() != null) {
            Optional<Destination> existing = destinationRepository.findByOsmTypeAndOsmId(
                destination.getOsmType(), destination.getOsmId()
            );
            if (existing.isPresent()) {
                return ResponseEntity.ok(existing.get());
            }
        }
        Destination saved = destinationRepository.save(destination);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Destination> getDestinations() {
        return destinationRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Destination> getDestinationById(@PathVariable Long id) {
        return destinationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDestination(@PathVariable Long id) {
        tripDestinationRepository.deleteByDestinationId(id);
        destinationRepository.deleteById(id);
        return ResponseEntity.ok("Destination deleted successfully");
    }
}