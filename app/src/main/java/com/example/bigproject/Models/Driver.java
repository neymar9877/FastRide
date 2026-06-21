package com.example.bigproject.Models;

import java.io.Serializable;

/**
 * Model class representing a driver in the system.
 * Holds driver-specific data: current location and GPS coordinates.
 * Corresponds to the 'drivers' table in Supabase.
 * Each Driver has the same id as their corresponding User.
 */
public class Driver implements Serializable {
    private String id;
    private String status; // "available" | "driving"
    private double current_lat;
    private double current_lng;

    /** Task: empty constructor required by Gson for deserialization. */
    public Driver() {}

    /**
     * Task: constructs a Driver with basic fields (no GPS coordinates).
     * Input: id (String), status (String)
     * Output: Driver instance
     */
    public Driver(String id, String status) {
        this.id = id;
        this.status = status;
    }

    /**
     * Task: constructs a Driver with full fields including GPS coordinates.
     * Input: id, status, current_lat (double), current_lng (double)
     * Output: Driver instance with all fields set
     */
    public Driver(String id, String status, double current_lat, double current_lng) {
        this.id = id;
        this.status = status;
        this.current_lat = current_lat;
        this.current_lng = current_lng;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getCurrentLat() { return current_lat; }
    public void setCurrentLat(double current_lat) { this.current_lat = current_lat; }

    public double getCurrentLng() { return current_lng; }
    public void setCurrentLng(double current_lng) { this.current_lng = current_lng; }
}
