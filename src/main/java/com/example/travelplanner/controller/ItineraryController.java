package com.example.travelplanner.controller;

import com.example.travelplanner.entity.Itinerary;
import com.example.travelplanner.repository.ItineraryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itinerary")
@CrossOrigin
public class ItineraryController {

    private final ItineraryRepository itineraryRepository;

    public ItineraryController(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
    }

    @PostMapping
    public Itinerary addItinerary(@RequestBody Itinerary itinerary) {
        return itineraryRepository.save(itinerary);
    }

    @GetMapping
    public List<Itinerary> getItinerary() {
        return itineraryRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public String deleteItinerary(@PathVariable Long id) {
        itineraryRepository.deleteById(id);
        return "Itinerary deleted successfully";
    }
}