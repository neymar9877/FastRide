package com.example.bigproject.UserFragments;

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

import com.example.bigproject.Activities.MainActivity;
import com.example.bigproject.R;
import com.example.bigproject.Repositories.BaseRepo;
import com.example.bigproject.Repositories.DriverRepo;
import com.example.bigproject.Repositories.RideRepo;
import com.example.bigproject.Models.RideRequest;

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

    // Task: Inflates the fragment layout representing the passenger's live map view.
    // Input: inflater (LayoutInflater), container (ViewGroup), savedInstanceState (Bundle)
    // Output: View
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank2, container, false);
    }

    // Task: Binds UI maps components, configures map touches, retrieves the active ride ID from shared storage, and initiates the data loading process.
    // Input: view (View), savedInstanceState (Bundle)
    // Output: None
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

    // Task: Loads ride parameters from the repository, focuses the map camera on the pickup coordinates, and determines whether to immediately show the route or wait for acceptance.
    // Input: rideId (String)
    // Output: None
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

    // Task: Runs a background loop that queries the database every 3 seconds to detect when a driver accepts the passenger's request.
    // Input: rideId (String)
    // Output: None
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

    // Task: Sets the screen state to the pickup phase, fetches the driver's current coordinates, and plots the route toward the passenger.
    // Input: ride (RideRequest)
    // Output: None
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

    // Task: Transitions the application state to the dropoff phase and updates the map to track the route from the pickup point to the destination.
    // Input: None
    // Output: None
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

    // Task: Adjusts map perspective, positions the starting and ending location markers, and calls the routing calculations.
    // Input: fromLat (double), fromLng (double), toLat (double), toLng (double), fromTitle (String), toTitle (String)
    // Output: None
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

    // Task: Calculates the real road route using an asynchronous OSRM road network threat, instantiates colored polylines, and forwards path points to the vehicle animator.
    // Input: start (GeoPoint), end (GeoPoint)
    // Output: None
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

    // Task: Configures a vehicle map marker at the origin point and initiates a timed loop that steps the marker smoothly along the computed route.
    // Input: routePoints (List<GeoPoint>)
    // Output: None
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

    // Task: Evaluates the finished path; if pickup concludes, triggers the dropoff phase after 5 seconds; if dropoff concludes, deletes session files and redirects back to ordering.
    // Input: None
    // Output: None
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

    // Task: Resumes map graphic rendering when the fragment shifts into the foreground.
    // Input: None
    // Output: None
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    // Task: Suspends map operations and safely removes pending polling and animation runnables to avoid background resource consumption.
    // Input: None
    // Output: None
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (carHandler != null && carAnimator != null) carHandler.removeCallbacks(carAnimator);
        if (pollHandler != null && pollRunnable != null) pollHandler.removeCallbacks(pollRunnable);
    }
}