package com.example.bigproject;

import java.io.Serializable;

public class Driver implements Serializable {
    private String id;
    private String current_location;
    private String status; // "available" | "driving"
    private double current_lat;
    private double current_lng;

    public Driver() {}

    public Driver(String id, String current_location, String status) {
        this.id = id;
        this.current_location = current_location;
        this.status = status;
    }

    public Driver(String id, String current_location, String status, double current_lat, double current_lng) {
        this.id = id;
        this.current_location = current_location;
        this.status = status;
        this.current_lat = current_lat;
        this.current_lng = current_lng;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCurrentLocation() { return current_location; }
    public void setCurrentLocation(String current_location) { this.current_location = current_location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getCurrentLat() { return current_lat; }
    public void setCurrentLat(double current_lat) { this.current_lat = current_lat; }

    public double getCurrentLng() { return current_lng; }
    public void setCurrentLng(double current_lng) { this.current_lng = current_lng; }
}