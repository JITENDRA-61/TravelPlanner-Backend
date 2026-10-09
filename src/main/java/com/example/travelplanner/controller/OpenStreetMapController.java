package com.example.travelplanner.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.travelplanner.dto.PlaceDto;
import com.example.travelplanner.service.OpenStreetMapService;

@RestController
@RequestMapping("/api/places")
@CrossOrigin
public class OpenStreetMapController {

    private final OpenStreetMapService openStreetMapService;

    public OpenStreetMapController(OpenStreetMapService openStreetMapService) {
        this.openStreetMapService = openStreetMapService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlaceDto>> searchPlaces(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "all") String category
    ) {
        List<PlaceDto> places = openStreetMapService.searchPlaces(query, category);
        return ResponseEntity.ok(places);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<PlaceDto>> nearbyPlaces(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "10000") Integer radius
    ) {
        // Accept either 'category' or 'type' parameter
        String targetCategory = (category != null && !category.isEmpty()) ? category : type;
        List<PlaceDto> places = openStreetMapService.getNearbyPlaces(latitude, longitude, targetCategory, radius);
        return ResponseEntity.ok(places);
    }
}
