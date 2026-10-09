package com.example.travelplanner.dto;

import com.example.travelplanner.entity.Destination;

public class TripDestinationDetailDto {
    private Long id;
    private Long tripId;
    private Long destinationId;
    private String visitDate;
    private String notes;
    private Destination destination;

    public TripDestinationDetailDto() {
    }

    public TripDestinationDetailDto(Long id, Long tripId, Long destinationId,
                                  String visitDate, String notes, Destination destination) {
        this.id = id;
        this.tripId = tripId;
        this.destinationId = destinationId;
        this.visitDate = visitDate;
        this.notes = notes;
        this.destination = destination;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }
}
