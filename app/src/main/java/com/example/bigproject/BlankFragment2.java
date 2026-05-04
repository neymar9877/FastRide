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

    private List<GeoPoint> carPath;
    private int carIndex = 0;
    private final Handler carHandler = new Handler(Looper.getMainLooper());
    private Runnable carAnimator;

    private Marker startMarker, endMarker, carMarker;

    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean rideStarted = false;

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
            tvStatus.setText("⏳ Waiting for driver to accept...");
            startPollingForAcceptance(activeRideId);
        } else {
            tvStatus.setText("No active ride. Order a ride first.");
        }
    }

    private void startPollingForAcceptance(String rideId) {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || rideStarted) return;
                RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
                    @Override
                    public void onSuccess(RideRequest ride) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            String status = ride.getStatus();
                            if ("accepted".equals(status) || "on_the_way".equals(status)) {
                                rideStarted = true;
                                pollHandler.removeCallbacks(pollRunnable);
                                tvStatus.setText("🚗 Driver is on the way!");
                                showRoute(ride);
                            } else if ("finished".equals(status)) {
                                rideStarted = true;
                                pollHandler.removeCallbacks(pollRunnable);
                                tvStatus.setText("✅ Ride complete!");
                            } else {
                                pollHandler.postDelayed(pollRunnable, 3000);
                            }
                        });
                    }
                    @Override
                    public void onError(Exception error) {
                        if (!isAdded()) return;
                        pollHandler.postDelayed(pollRunnable, 3000);
                    }
                });
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void showRoute(RideRequest ride) {
        GeoPoint pickupPoint = new GeoPoint(ride.getPickupLat(), ride.getPickupLng());
        GeoPoint dropoffPoint = new GeoPoint(ride.getDropoffLat(), ride.getDropoffLng());

        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(pickupPoint);

        if (startMarker == null) {
            startMarker = new Marker(mapView);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(getResources().getDrawable(R.drawable.baseline_place_24));
            startMarker.setTitle("📍 Your Pickup");
            mapView.getOverlays().add(startMarker);
        }
        startMarker.setPosition(pickupPoint);

        if (endMarker == null) {
            endMarker = new Marker(mapView);
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            endMarker.setIcon(getResources().getDrawable(R.drawable.baseline_place_24));
            endMarker.setTitle("🏁 Your Destination");
            mapView.getOverlays().add(endMarker);
        }
        endMarker.setPosition(dropoffPoint);

        drawRoute(pickupPoint, dropoffPoint);
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(start);
        waypoints.add(end);

        RoadManager roadManager = new OSRMRoadManager(getContext(), "ANDROID");

        new Thread(() -> {
            Road road = roadManager.getRoad(waypoints);
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
            roadOverlay.setWidth(12f);
            roadOverlay.setColor(0xFF1E88E5);

            List<GeoPoint> routePoints = roadOverlay.getActualPoints();

            requireActivity().runOnUiThread(() -> {
                mapView.getOverlays().add(roadOverlay);
                mapView.invalidate();

                if (routePoints != null && routePoints.size() > 1) {
                    startCarAnimation(routePoints);
                }
            });
        }).start();
    }

    private void startCarAnimation(List<GeoPoint> routePoints) {
        carPath = routePoints;
        carIndex = 0;

        if (carMarker == null) {
            carMarker = new Marker(mapView);
            carMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            carMarker.setTitle("🚗 Your Driver");
            carMarker.setIcon(getResources().getDrawable(R.drawable.baseline_directions_car_24));
            mapView.getOverlays().add(carMarker);
        }

        carMarker.setPosition(carPath.get(0));
        mapView.getController().setCenter(carPath.get(0));
        mapView.invalidate();

        carAnimator = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || carPath == null || carIndex >= carPath.size()) {
                    if (isAdded()) {
                        tvStatus.setText("✅ You have arrived!");
                        Toast.makeText(getContext(), "You have arrived at your destination!", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                GeoPoint nextPoint = carPath.get(carIndex);
                carIndex++;
                carMarker.setPosition(nextPoint);
                mapView.getController().setCenter(nextPoint);
                mapView.invalidate();
                carHandler.postDelayed(this, 300);
            }
        };

        carHandler.post(carAnimator);
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
        if (carHandler != null && carAnimator != null) carHandler.removeCallbacks(carAnimator);
        if (pollHandler != null && pollRunnable != null) pollHandler.removeCallbacks(pollRunnable);
    }
}