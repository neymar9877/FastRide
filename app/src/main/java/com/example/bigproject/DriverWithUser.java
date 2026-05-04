package com.example.bigproject;

public class DriverWithUser {

    private String id;
    private String current_location;
    private String status;
    private User users;
    private double current_lat;
    private double current_lng;
    private double distanceKm = -1; // calculated client-side, not stored in DB

    public DriverWithUser() {}

    public void setId(String id) { this.id = id; }
    public void setCurrentLocation(String current_location) { this.current_location = current_location; }
    public void setStatus(String status) { this.status = status; }
    public void setUsers(User users) { this.users = users; }
    public void setCurrentLat(double current_lat) { this.current_lat = current_lat; }
    public void setCurrentLng(double current_lng) { this.current_lng = current_lng; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getId() { return id; }
    public String getCurrentLocation() { return current_location; }
    public String getStatus() { return status; }
    public User getUsers() { return users; }
    public double getCurrentLat() { return current_lat; }
    public double getCurrentLng() { return current_lng; }
    public double getDistanceKm() { return distanceKm; }

    public Driver toDriver() {
        return new Driver(this.id, this.current_location, this.status, this.current_lat, this.current_lng);
    }
}
