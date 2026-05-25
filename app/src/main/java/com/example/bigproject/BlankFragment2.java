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
    private Polyline pastRouteOverlay, remainingOverlay, routeOverlay;

    private RideRequest currentRide;

    // Polling while waiting for driver to accept
    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean animationStarted = false;

    private enum Phase { PICKUP, DROPOFF }
    private Phase currentPhase = Phase.PICKUP;

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
            loadRideAndWait(activeRideId);
        } else {
            tvStatus.setText("No active ride. Order a ride first.");
        }
    }

    // Step 1: load ride data, then start polling for acceptance
    private void loadRideAndWait(String rideId) {
        RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
            @Override
            public void onSuccess(RideRequest ride) {
                if (!isAdded()) return;
                currentRide = ride;
                requireActivity().runOnUiThread(() -> {

                    // Set map to pickup location immediately so it's not blank
                    mapView.getController().setZoom(15.0);
                    mapView.getController().setCenter(
                            new GeoPoint(ride.getPickupLat(), ride.getPickupLng())
                    );

                    // If already accepted, go straight to animation
                    if ("accepted".equals(ride.getStatus()) || "on_the_way".equals(ride.getStatus())) {
                        startPhase1(ride);
                    } else {
                        startPollingForAcceptance(rideId);
                    }
                });
            }
            @Override
            public void onError(Exception error) {
                Log.e("PassengerMap", "Failed to load ride: " + error.getMessage());
            }
        });
    }

    // Step 2: poll every 3 seconds until driver accepts
    private void startPollingForAcceptance(String rideId) {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || animationStarted) return;

                RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
                    @Override
                    public void onSuccess(RideRequest ride) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if ("accepted".equals(ride.getStatus())) {
                                animationStarted = true;
                                pollHandler.removeCallbacks(pollRunnable);
                                startPhase1(ride);
                            } else if ("finished".equals(ride.getStatus())) {
                                animationStarted = true;
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

    // Phase 1: driver going to pickup — fetch driver location and show route
    private void startPhase1(RideRequest ride) {
        currentPhase = Phase.PICKUP;
        tvStatus.setText("🚗 Driver is on the way to pick you up!");

        DriverRepo driverRepo = new DriverRepo();
        driverRepo.getDriverLocation(ride.getDriverId(), new BaseRepo.RepoCallback<double[]>() {
            @Override
            public void onSuccess(double[] latLng) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        showRoute(
                                latLng[0], latLng[1],
                                ride.getPickupLat(), ride.getPickupLng(),
                                "🚗 Driver", "📍 Your Pickup"
                        )
                );
            }
            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                // Fallback: if can't get driver location, just show pickup→dropoff
                requireActivity().runOnUiThread(() -> startPhase2());
            }
        });
    }

    // Phase 2: driving to destination
    private void startPhase2() {
        if (!isAdded() || currentRide == null) return;
        currentPhase = Phase.DROPOFF;
        tvStatus.setText("🚗 You're on your way to the destination!");
        showRoute(
                currentRide.getPickupLat(), currentRide.getPickupLng(),
                currentRide.getDropoffLat(), currentRide.getDropoffLng(),
                "📍 Pickup", "🏁 Destination"
        );
    }

    // Same showRoute as DriverMapFragment
    private void showRoute(double fromLat, double fromLng,
                           double toLat, double toLng,
                           String fromTitle, String toTitle) {
        GeoPoint fromPoint = new GeoPoint(fromLat, fromLng);
        GeoPoint toPoint = new GeoPoint(toLat, toLng);

        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(fromPoint);

        if (startMarker == null) {
            startMarker = new Marker(mapView);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(getResources().getDrawable(R.drawable.baseline_place_24));
            mapView.getOverlays().add(startMarker);
        }
        startMarker.setTitle(fromTitle);
        startMarker.setPosition(fromPoint);

        if (endMarker == null) {
            endMarker = new Marker(mapView);
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            endMarker.setIcon(getResources().getDrawable(R.drawable.baseline_place_24));
            mapView.getOverlays().add(endMarker);
        }
        endMarker.setTitle(toTitle);
        endMarker.setPosition(toPoint);

        drawRoute(fromPoint, toPoint);
    }

    // Same drawRoute as DriverMapFragment
    private void drawRoute(GeoPoint start, GeoPoint end) {
        if (routeOverlay != null) mapView.getOverlays().remove(routeOverlay);
        if (pastRouteOverlay != null) mapView.getOverlays().remove(pastRouteOverlay);
        if (remainingOverlay != null) mapView.getOverlays().remove(remainingOverlay);

        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(start);
        waypoints.add(end);

        RoadManager roadManager = new OSRMRoadManager(getContext(), "ANDROID");

        new Thread(() -> {
            Road road = roadManager.getRoad(waypoints);

            requireActivity().runOnUiThread(() -> {
                routeOverlay = RoadManager.buildRoadOverlay(road);
                routeOverlay.setWidth(12f);

                pastRouteOverlay = new Polyline();
                pastRouteOverlay.setWidth(12f);
                pastRouteOverlay.setColor(0xFF888888); // gray

                remainingOverlay = new Polyline();
                remainingOverlay.setWidth(12f);
                remainingOverlay.setColor(0xFF1E88E5); // blue

                remainingOverlay.setPoints(routeOverlay.getActualPoints());

                mapView.getOverlays().add(remainingOverlay);
                mapView.getOverlays().add(pastRouteOverlay);
                mapView.getOverlays().add(routeOverlay);
                mapView.invalidate();

                List<GeoPoint> points = routeOverlay.getActualPoints();
                if (points != null && points.size() > 1) {
                    startCarAnimation(points);
                }
            });
        }).start();
    }

    // Same startCarAnimation as DriverMapFragment
    private void startCarAnimation(List<GeoPoint> routePoints) {
        if (carAnimator != null) carHandler.removeCallbacks(carAnimator);
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
                if (!isAdded()) return;
                if (carPath == null || carIndex >= carPath.size()) {
                    onAnimationFinished();
                    return;
                }

                GeoPoint nextPoint = carPath.get(carIndex);
                carIndex++;

                carMarker.setPosition(nextPoint);
                mapView.getController().setCenter(nextPoint);

                List<GeoPoint> past = new ArrayList<>(carPath.subList(0, carIndex));
                List<GeoPoint> remaining = new ArrayList<>(carPath.subList(carIndex, carPath.size()));
                pastRouteOverlay.setPoints(past);
                remainingOverlay.setPoints(remaining);

                mapView.invalidate();
                carHandler.postDelayed(this, 300);
            }
        };

        carHandler.post(carAnimator);
    }

    private void onAnimationFinished() {
        if (currentPhase == Phase.PICKUP) {
            tvStatus.setText("🎉 Driver arrived! Get in the car.");
            Toast.makeText(getContext(), "Your driver has arrived!", Toast.LENGTH_LONG).show();

            // Wait 5 seconds then show the ride to destination
            carHandler.postDelayed(() -> {
                if (isAdded()) startPhase2();
            }, 5000);

        } else {
            tvStatus.setText("✅ You have arrived!");
            Toast.makeText(getContext(), "You have arrived at your destination!", Toast.LENGTH_LONG).show();

            // Clear active ride
            SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
            sp.edit().remove("activeRideId").apply();

            // Wait 5 seconds then go back to orders fragment
            carHandler.postDelayed(() -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).goToOrdersFragment();
                    }
                });
            }, 5000);
        }
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