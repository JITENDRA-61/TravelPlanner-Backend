package com.example.travelplanner.controller;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@CrossOrigin
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("app", "Group Travel Itinerary Planner");
        status.put("status", "UP");

        try (var conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            status.put("database", valid ? "CONNECTED" : "UNAVAILABLE");
            status.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            status.put("database", "DISCONNECTED");
            status.put("databaseError", e.getMessage());
        }

        return ResponseEntity.ok(status);
    }
}
