package com.example.bigproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.util.ArrayList;
import java.util.List;

public class BlankFragment2 extends Fragment {

    private MapView mapView;
    private TextView tvStatus;

    private Marker driverMarker, pickupMarker, dropoffMarker;
    private Polyline routeOverlay;

    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    private RideRequest currentRide;
    private String driverId;
    private boolean routeDrawn = false;
    private String lastStatus = "";

    public BlankFragment2() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.osm_map);
        tvStatus = view.findViewById(R.id.tvMapStatus);

        mapView.setMultiTouchControls(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        String activeRideId = sp.getString("activeRideId", null);

        if (activeRideId != null) {
            loadRideData(activeRideId);
        } else {
            tvStatus.setText("No active ride. Order a ride first.");
        }
    }

    private void loadRideData(String rideId) {
        tvStatus.setText("⏳ Loading ride info...");
        RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
            @Override
            public void onSuccess(RideRequest ride) {
                if (!isAdded()) return;
                currentRide = ride;
                driverId = ride.getDriverId();
                requireActivity().runOnUiThread(() -> startPolling());
            }
            @Override
            public void onError(Exception error) {
                Log.e("PassengerMap", "Failed to load ride: " + error.getMessage());
            }
        });
    }

    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;

                // Step 1: get driver's current location
                DriverRepo driverRepo = new DriverRepo();
                driverRepo.getDriverLocation(driverId, new BaseRepo.RepoCallback<double[]>() {
                    @Override
                    public void onSuccess(double[] latLng) {
                        if (!isAdded()) return;

                        // Step 2: get ride status
                        SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
                        String rideId = sp.getString("activeRideId", null);
                        if (rideId == null) return;

                        RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
                            @Override
                            public void onSuccess(RideRequest ride) {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    String status = ride.getStatus();
                                    GeoPoint driverPos = new GeoPoint(latLng[0], latLng[1]);

                                    // Update driver marker position
                                    updateDriverMarker(driverPos);

                                    if ("accepted".equals(status)) {
                                        tvStatus.setText("🚗 Driver is coming to pick you up...");

                                        // Draw route driver → pickup (only once per phase)
                                        if (!lastStatus.equals("accepted")) {
                                            lastStatus = "accepted";
                                            routeDrawn = false;
                                            drawRoute(
                                                    driverPos,
                                                    new GeoPoint(currentRide.getPickupLat(), currentRide.getPickupLng())
                                            );
                                            setupMarkers();
                                        }
                                        pollHandler.postDelayed(pollRunnable, 2000);

                                    } else if ("on_the_way".equals(status)) {
                                        tvStatus.setText("🚗 You're on your way!");

                                        // Switch to route pickup → dropoff (only once)
                                        if (!lastStatus.equals("on_the_way")) {
                                            lastStatus = "on_the_way";
                                            routeDrawn = false;
                                            drawRoute(
                                                    new GeoPoint(currentRide.getPickupLat(), currentRide.getPickupLng()),
                                                    new GeoPoint(currentRide.getDropoffLat(), currentRide.getDropoffLng())
                                            );
                                            setupMarkers();
                                        }
                                        pollHandler.postDelayed(pollRunnable, 2000);

                                    } else if ("finished".equals(status)) {
                                        tvStatus.setText("✅ You have arrived!");
                                        Toast.makeText(getContext(), "You have arrived at your destination!", Toast.LENGTH_LONG).show();
                                        // Stop polling
                                    } else {
                                        pollHandler.postDelayed(pollRunnable, 2000);
                                    }
                                });
                            }
                            @Override
                            public void onError(Exception error) {
                                if (!isAdded()) return;
                                pollHandler.postDelayed(pollRunnable, 2000);
                            }
                        });
                    }
                    @Override
                    public void onError(Exception error) {
                        if (!isAdded()) return;
                        pollHandler.postDelayed(pollRunnable, 2000);
                    }
                });
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void updateDriverMarker(GeoPoint position) {
        if (driverMarker == null) {
            driverMarker = new Marker(mapView);
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            driverMarker.setTitle("🚗 Your Driver");
            driverMarker.setIcon(getResources().getDrawable(R.drawable.baseline_directions_car_24));
            mapView.getOverlays().add(driverMarker);
        }
        driverMarker.setPosition(position);
        mapView.getController().setCenter(position);
        mapView.invalidate();
    }

    private void setupMarkers() {
        // Pickup marker
        if (pickupMarker == null) {
            pickupMarker = new Marker(mapView);
            pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            pickupMarker.setIcon(getResources().getDrawable(R.drawable.baseline_place_24));
            pickupMarker.setTitle("📍 Pickup");
            mapView.getOverlays().add(pickupMarker);
        }
        pickupMarker.setPosition(new GeoPoint(currentRide.getPickupLat(), currentRide.getPickupLng()));

        // Dropoff marker
        if (dropoffMarker == null) {
            dropoffMarker = new Marker(mapView);
            dropoffMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            dropoffMarker.setIcon(getResources().getDrawable(R.drawable.baseline_place_24));
            dropoffMarker.setTitle("🏁 Destination");
            mapView.getOverlays().add(dropoffMarker);
        }
        dropoffMarker.setPosition(new GeoPoint(currentRide.getDropoffLat(), currentRide.getDropoffLng()));

        mapView.invalidate();
    }

    private void drawRoute(GeoPoint from, GeoPoint to) {
        if (routeOverlay != null) mapView.getOverlays().remove(routeOverlay);

        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(from);
        waypoints.add(to);

        RoadManager roadManager = new OSRMRoadManager(getContext(), "ANDROID");

        new Thread(() -> {
            Road road = roadManager.getRoad(waypoints);
            Polyline overlay = RoadManager.buildRoadOverlay(road);
            overlay.setWidth(10f);
            overlay.setColor(0xFF1E88E5); // blue

            requireActivity().runOnUiThread(() -> {
                if (routeOverlay != null) mapView.getOverlays().remove(routeOverlay);
                routeOverlay = overlay;
                mapView.getOverlays().add(routeOverlay);
                mapView.invalidate();
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (pollHandler != null && pollRunnable != null) pollHandler.removeCallbacks(pollRunnable);
    }
}