package com.example.bigproject.Models;

/**
 * Model class representing a ride request.
 * Created when a passenger selects a driver and confirms a ride.
 * Corresponds to the 'rides' table in Supabase.
 */
public class RideRequest {

    private String id;
    private String passenger_id;
    private String driver_id;
    private double pickup_lat;
    private double pickup_lng;
    private double dropoff_lat;
    private double dropoff_lng;
    private String status; // requested | accepted | on_the_way | finished | declined
    private String created_at;

    /** Task: empty constructor required by Gson for deserialization. */
    public RideRequest() {}

    /**
     * Task: constructs a complete RideRequest object.
     * Input: id, passengerId, driverId, pickupLat, pickupLng, dropoffLat, dropoffLng, status
     * Output: RideRequest instance with all fields set
     */
    public RideRequest(String id, String passengerId, String driverId,
                       double pickupLat, double pickupLng,
                       double dropoffLat, double dropoffLng, String status) {
        this.id = id;
        this.driver_id = driverId;
        this.passenger_id = passengerId;
        this.pickup_lat = pickupLat;
        this.pickup_lng = pickupLng;
        this.dropoff_lat = dropoffLat;
        this.dropoff_lng = dropoffLng;
        this.status = status;
    }

    public String getId() { return id; }
    public String getPassengerId() { return passenger_id; }
    public String getDriverId() { return driver_id; }
    public double getPickupLat() { return pickup_lat; }
    public double getPickupLng() { return pickup_lng; }
    public double getDropoffLat() { return dropoff_lat; }
    public double getDropoffLng() { return dropoff_lng; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return created_at; }

    public void setId(String id) { this.id = id; }
    public void setPassengerId(String passengerId) { this.passenger_id = passengerId; }
    public void setDriverId(String DriverId) { this.driver_id = DriverId; }
    public void setPickupLat(double pickupLat) { this.pickup_lat = pickupLat; }
    public void setPickupLng(double pickupLng) { this.pickup_lng = pickupLng; }
    public void setDropoffLat(double dropoffLat) { this.dropoff_lat = dropoffLat; }
    public void setDropoffLng(double dropoffLng) { this.dropoff_lng = dropoffLng; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(String created_at) { this.created_at = created_at; }
}
