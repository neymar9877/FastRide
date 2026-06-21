package com.example.bigproject.Models;

/**
 * Helper model class that combines Driver and User data.
 * Used when displaying driver lists to passengers or admins,
 * where both driver-specific fields (location, status) and
 * user fields (name, image) are needed together.
 * Also holds distanceKm which is calculated client-side, not stored in DB.
 */
public class DriverWithUser {

    private String id;
    private String status;
    private User users;
    private double current_lat;
    private double current_lng;
    private double distanceKm = -1; // calculated client-side, not stored in DB
    private double estimatedPrice = -1; // מחיר משוער לנסיעה, מחושב ב-BlankFragment3

    /** Task: empty constructor required for manual instantiation. */
    public DriverWithUser() {}

    /**
    * SETTERES
    * */

    public void setId(String id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setUsers(User users) { this.users = users; }
    public void setCurrentLat(double current_lat) { this.current_lat = current_lat; }
    public void setCurrentLng(double current_lng) { this.current_lng = current_lng; }
    public void setEstimatedPrice(double estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    /**
     * Task: sets the calculated distance in kilometers from passenger to this driver.
     * Input: distanceKm (double) — distance calculated using Location.distanceBetween()
     * Output: none
     */
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    /**
     * GETTERES
     * */

    public String getId() { return id; }
    public String getStatus() { return status; }
    public User getUsers() { return users; }
    public double getCurrentLat() { return current_lat; }
    public double getCurrentLng() { return current_lng; }
    public double getDistanceKm() { return distanceKm; }
    public double getEstimatedPrice() { return estimatedPrice; }

    /**
     * Task: converts this DriverWithUser to a plain Driver object.
     * Input: none
     * Output: Driver instance with id, location, status, lat, lng
     */
    public Driver toDriver() {
        return new Driver(this.id, this.status, this.current_lat, this.current_lng);
    }
}
