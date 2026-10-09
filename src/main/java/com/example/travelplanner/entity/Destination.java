package com.example.travelplanner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "destinations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_osm_type_id", columnNames = {"osmType", "osmId"})
    }
)
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String place; // Kept for backwards compatibility
    private String city;
    private String state;
    private String country;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;
    private Double latitude;
    private Double longitude;

    private String osmType;
    private String osmId;
    private String visitDate; // Kept for backwards compatibility

    public Destination() {
    }

    public Destination(String name, String city, String state, String country,
                       String description, String category, Double latitude,
                       Double longitude, String osmType, String osmId) {
        this.name = name;
        this.place = name;
        this.city = city;
        this.state = state;
        this.country = country;
        this.description = description;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.osmType = osmType;
        this.osmId = osmId;
    }

    public Destination(String place, String description, String visitDate) {
        this.place = place;
        this.name = place;
        this.description = description;
        this.visitDate = visitDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : place;
    }

    public void setName(String name) {
        this.name = name;
        if (this.place == null) {
            this.place = name;
        }
    }

    public String getPlace() {
        return place != null ? place : name;
    }

    public void setPlace(String place) {
        this.place = place;
        if (this.name == null) {
            this.name = place;
        }
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getOsmType() {
        return osmType;
    }

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    public String getOsmId() {
        return osmId;
    }

    public void setOsmId(String osmId) {
        this.osmId = osmId;
    }

    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }
}