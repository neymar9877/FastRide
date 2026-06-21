package com.example.bigproject.driver_fragments;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.bigproject.Repositories.BaseRepo;
import com.example.bigproject.Activities.DriverActivity;
import com.example.bigproject.Repositories.DriverRepo;
import com.example.bigproject.R;
import com.example.bigproject.Repositories.RideRepo;
import com.example.bigproject.Models.RideRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;

import java.util.ArrayList;
import java.util.List;

public class DriverMapFragment extends Fragment {

    private MapView mapView;
    private List<GeoPoint> carPath;
    private int carIndex = 0;
    private final Handler carHandler = new Handler(Looper.getMainLooper());
    private Runnable carAnimator;

    private Marker startMarker, endMarker, carMarker;
    private Polyline pastRouteOverlay, remainingOverlay, routeOverlay;

    private String driverId;
    private RideRequest currentRide;

    // FIX 2: location updates
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DriverRepo driverRepo;

    private enum Phase { PICKUP, DROPOFF }
    private Phase currentPhase = Phase.PICKUP;

    public DriverMapFragment() {}

    // Task: Inflates the fragment layout representing the driver's active transit navigation map view.
    // Input: inflater (LayoutInflater), container (ViewGroup), savedInstanceState (Bundle)
    // Output: View
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_map, container, false);
    }

    // Task: Initializes UI elements, prepares mapping engines, pulls session metadata, and boots automatic positioning updates if a trip is ongoing.
    // Input: view (View), savedInstanceState (Bundle)
    // Output: None
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.osm_map);
        mapView.setMultiTouchControls(true);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);

        driverRepo = new DriverRepo();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        driverId = sp.getString("userId", null);
        String activeRideId = sp.getString("activeRideId", null);

        if (activeRideId != null) {
            startLocationUpdates(); // FIX 2: start updating GPS every 2 seconds
            loadRideAndStart(activeRideId);
        } else {
            Toast.makeText(getContext(), "No active ride found", Toast.LENGTH_SHORT).show();
        }
    }

    // Task: Establishes a recurring Google LocationRequest checking interval tracking physical device motion every 2000 milliseconds to stream coordinates to Supabase.
    // Input: None
    // Output: None
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = new LocationRequest.Builder(2000)
                .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || driverId == null) return;
                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {

                    // Read the current ride status from SharedPreferences
                    // to pass the correct status — never overwrite "on_the_way" with "available"
                    SharedPreferences sp = requireContext()
                            .getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
                    String currentStatus = sp.getString("driverStatus", "available");

                    driverRepo.updateDriverLocation(driverId,
                            location.getLatitude(),
                            location.getLongitude(),
                            currentStatus, // pass status instead of hardcoding "available"
                            new BaseRepo.RepoCallback<Boolean>() {
                                @Override public void onSuccess(Boolean result) {}
                                @Override public void onError(Exception error) {
                                    Log.e("DriverMap", "Location update failed: " + error.getMessage());
                                }
                            });
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    // Task: Safe tear-down function that commands the FusedLocationProviderClient to disconnect active callbacks and stop GPS tracking.
    // Input: None
    // Output: None
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // Task: Loads trip records via RideRepo, detects the driver's origin coordinates, and initiates the pickup phase layout mapping.
    // Input: rideId (String)
    // Output: None
    private void loadRideAndStart(String rideId) {
        RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
            @Override
            public void onSuccess(RideRequest ride) {
                if (!isAdded()) return;
                currentRide = ride;

                driverRepo.getDriverLocation(driverId, new BaseRepo.RepoCallback<double[]>() {
                    @Override
                    public void onSuccess(double[] latLng) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            currentPhase = Phase.PICKUP;
                            showRoute(
                                    latLng[0], latLng[1],
                                    ride.getPickupLat(), ride.getPickupLng(),
                                    "Your Location", "📍 Pickup Point"
                            );
                        });
                    }
                    @Override
                    public void onError(Exception error) {
                        Log.e("DriverMap", "Failed to get driver location: " + error.getMessage());
                    }
                });
            }
            @Override
            public void onError(Exception error) {
                Log.e("DriverMap", "Failed to load ride: " + error.getMessage());
            }
        });
    }

    // Task: Configures camera focus zoom details, handles source/target pin allocations, and sends locations forward to overlay calculations.
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

    // Task: Calculates actual routing data using an external thread worker via OSRMRoadManager, instantiates split polyline displays, and initializes the vehicle animator.
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

                // FIX 1: gray for past route
                pastRouteOverlay = new Polyline();
                pastRouteOverlay.setWidth(12f);
                pastRouteOverlay.setColor(0x808080); // gray

                // blue for remaining route
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

    // Task: Instantiates a car marker icon over the map canvas and fires a periodic thread loop that updates vehicle tracking positions and modifies past/future line metrics.
    // Input: routePoints (List<GeoPoint>)
    // Output: None
    private void startCarAnimation(List<GeoPoint> routePoints) {
        if (carAnimator != null) carHandler.removeCallbacks(carAnimator);
        carPath = routePoints;
        carIndex = 0;

        if (carMarker == null) {
            carMarker = new Marker(mapView);
            carMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            carMarker.setTitle("Driver");
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

                // Update Supabase with car's current position on the route
                SharedPreferences sp = requireContext()
                        .getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
                String currentStatus = sp.getString("driverStatus", "on_the_way");

                driverRepo.updateDriverLocation(driverId, nextPoint.getLatitude(), nextPoint.getLongitude(), currentStatus,
                        new BaseRepo.RepoCallback<Boolean>() {
                            @Override public void onSuccess(Boolean result) {}
                            @Override public void onError(Exception error) {}
                        });

                // FIX 1: update past (gray) and remaining (blue) overlays
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

    // Task: Resolves route targets; updates ride status to "on_the_way" after pickup and paths to destination; flags trip as "finished" upon dropoff, cleaning storage keys and resetting view frames.
    // Input: None
    // Output: None
    private void onAnimationFinished() {
        if (currentPhase == Phase.PICKUP) {
            Toast.makeText(getContext(), "🎉 Arrived at pickup! Passenger boarded.", Toast.LENGTH_LONG).show();

            SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
            String rideId = sp.getString("activeRideId", null);
            if (rideId != null) {
                RideRepo.updateRideStatus(rideId, "on_the_way", new BaseRepo.RepoCallback<Boolean>() {
                    @Override public void onSuccess(Boolean result) {}
                    @Override public void onError(Exception error) {}
                });
            }

            carHandler.postDelayed(() -> {
                if (!isAdded() || currentRide == null) return;
                currentPhase = Phase.DROPOFF;
                requireActivity().runOnUiThread(() ->
                        showRoute(
                                currentRide.getPickupLat(), currentRide.getPickupLng(),
                                currentRide.getDropoffLat(), currentRide.getDropoffLng(),
                                "📍 Pickup", "🏁 Destination"
                        )
                );
            }, 5000);

        } else {
            // FIX 3: arrived at dropoff
            Toast.makeText(getContext(), "✅ Ride complete! Arrived at destination.", Toast.LENGTH_LONG).show();

            stopLocationUpdates(); // FIX 2: stop GPS updates

            SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
            String rideId = sp.getString("activeRideId", null);
            if (rideId != null) {
                RideRepo.updateRideStatus(rideId, "finished", new BaseRepo.RepoCallback<Boolean>() {
                    @Override public void onSuccess(Boolean result) {}
                    @Override public void onError(Exception error) {}
                });
                sp.edit().remove("activeRideId").apply();
            }

            // FIX 3: wait 5 seconds then go back to home fragment
            carHandler.postDelayed(() -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (getActivity() instanceof DriverActivity) {
                        ((DriverActivity) getActivity()).goToHomeFragment();
                    }
                });
            }, 5000);
        }
    }

    // Task: Directs map canvas layers to re-activate processing threads when the view comes to the foreground.
    // Input: None
    // Output: None
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    // Task: Forces map operations to freeze, unties looping background animators, and drops active hardware GPS listeners to avoid runtime memory leaks.
    // Input: None
    // Output: None
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (carHandler != null && carAnimator != null) carHandler.removeCallbacks(carAnimator);
        stopLocationUpdates(); // FIX 2: stop GPS updates when fragment pauses
    }
}