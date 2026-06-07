package com.example.bigproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bigproject.GeoUtils.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlankFragment3 extends Fragment {

    private EditText etPickupAddress, etDropAddress;
    private Button btnFindDrivers, btnConfirmRide, btnLogout;
    private TextView tvRideStatus, tvDriversLabel;
    private RecyclerView recyclerDrivers;

    private DriverSelectAdapter driverAdapter;
    private ArrayList<DriverWithUser> driversList = new ArrayList<>();

    private DriverWithUser selectedDriver = null;
    private LatLng pickupLatLng = null;
    private LatLng dropLatLng = null;

    private String passengerId = null;

    public BlankFragment3() {}

    // Task: Inflates the layout representing the passenger's ride ordering interface.
    // Input: inflater (LayoutInflater), container (ViewGroup), savedInstanceState (Bundle)
    // Output: View
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank3, container, false);
    }

    // Task: Links layout XML components, configures the interactive driver selector adapter list, and binds functional click behaviors.
    // Input: view (View), savedInstanceState (Bundle)
    // Output: None
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPickupAddress = view.findViewById(R.id.etPickupAddress);
        etDropAddress = view.findViewById(R.id.etDropAddress);
        btnFindDrivers = view.findViewById(R.id.btnFindDrivers);
        btnConfirmRide = view.findViewById(R.id.btnConfirmRide);
        btnLogout = view.findViewById(R.id.btnLogout);
        tvRideStatus = view.findViewById(R.id.tvRideStatus);
        tvDriversLabel = view.findViewById(R.id.tvDriversLabel);
        recyclerDrivers = view.findViewById(R.id.recyclerDrivers);

        // Setup drivers list
        driverAdapter = new DriverSelectAdapter(driversList, driver -> {
            selectedDriver = driver;
            btnConfirmRide.setVisibility(View.VISIBLE);
            tvRideStatus.setText("Selected: " + (driver.getUsers() != null ? driver.getUsers().getUserName() : "Driver"));
        });
        recyclerDrivers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerDrivers.setAdapter(driverAdapter);

        // Load passenger ID from SharedPreferences
        loadPassengerId();

        btnFindDrivers.setOnClickListener(v -> findDrivers());
        btnConfirmRide.setOnClickListener(v -> confirmRide());
        btnLogout.setOnClickListener(v -> logout());
    }

    // Task: Directs an immediate local query to fetch the current unique active passenger identifier string from SharedPreferences storage.
    // Input: None
    // Output: None
    private void loadPassengerId() {
        SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);

        // Get the saved userId directly — no network call needed
        String savedId = sp.getString("userId", null);
        if (savedId != null) {
            passengerId = savedId;
        }
    }

    // Task: Launches an isolated background worker thread to safely transform written locations into numerical map coordinates via a geocoding process.
    // Input: None
    // Output: None
    private void findDrivers() {
        String pickupText = etPickupAddress.getText().toString().trim();
        String dropText = etDropAddress.getText().toString().trim();

        if (pickupText.isEmpty() || dropText.isEmpty()) {
            Toast.makeText(getContext(), "Fill both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        tvRideStatus.setText("Finding drivers...");
        btnFindDrivers.setEnabled(false);

        // Geocoding must run on background thread
        new Thread(() -> {
            pickupLatLng = GeoUtils.getLatLngFromAddress(getContext(), pickupText);
            dropLatLng = GeoUtils.getLatLngFromAddress(getContext(), dropText);

            new Handler(Looper.getMainLooper()).post(() -> {
                btnFindDrivers.setEnabled(true);

                if (pickupLatLng == null || dropLatLng == null) {
                    Toast.makeText(getContext(), "Address not found or outside Israel. Please enter a valid Israeli address.", Toast.LENGTH_LONG).show();
                    tvRideStatus.setText("");
                    return;
                }

                // Now fetch available drivers
                fetchAvailableDrivers();
            });
        }).start();
    }

    // Task: Fetches online drivers, runs mathematical calculations to find the distances between drivers and the pickup point, sorts them ascendingly, and populates the view list.
    // Input: None
    // Output: None
    private void fetchAvailableDrivers() {
        DriverRepo driverRepo = new DriverRepo();
        driverRepo.getAvailableDrivers(new BaseRepo.RepoCallback<List<DriverWithUser>>() {
            @Override
            public void onSuccess(List<DriverWithUser> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (result.isEmpty()) {
                        tvRideStatus.setText("No available drivers right now. Try again soon.");
                        return;
                    }

                    // Calculate distance for each driver
                    for (DriverWithUser driver : result) {
                        if (driver.getCurrentLat() != 0 && driver.getCurrentLng() != 0) {
                            float[] distToPickup  = new float[1];
                            Log.d("COORD_CHECK", "Pickup Lat/Lng: " + pickupLatLng.latitude + ", " + pickupLatLng.longitude);
                            Log.d("COORD_CHECK", "Driver Lat/Lng: " + driver.getCurrentLat() + ", " + driver.getCurrentLng());
                            Location.distanceBetween(
                                    pickupLatLng.latitude, pickupLatLng.longitude,
                                    driver.getCurrentLat(), driver.getCurrentLng(),
                                    distToPickup
                            );

                            double distDriverToPickupKm  = distToPickup [0] / 1000.0;
                            driver.setDistanceKm(distDriverToPickupKm );

                            // ride's distance
                            float[] distRide = new float[1];
                            Location.distanceBetween(
                                    pickupLatLng.latitude,  pickupLatLng.longitude,
                                    dropLatLng.latitude,    dropLatLng.longitude,
                                    distRide
                            );
                            double rideDistanceKm = distRide[0] / 1000.0;
                            // formula for ride price
                            double price = 10.0 + (rideDistanceKm * 3.5) + (distDriverToPickupKm * 1.5);
                            driver.setEstimatedPrice(price);

                            // LOG INSIDE THE LOOP so you see every driver
                            Log.d("DISTANCE_CHECK", "Driver: " + driver.toDriver().getId() + " | Dist: " + distDriverToPickupKm  + " km");
                        }
                    }
                    // Sort by distance
                    Collections.sort(result, (a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()));

                    driversList.clear();
                    driversList.addAll(result);
                    driverAdapter.notifyDataSetChanged();

                    // Show the list
                    tvDriversLabel.setVisibility(View.VISIBLE);
                    recyclerDrivers.setVisibility(View.VISIBLE);
                    tvRideStatus.setText("Tap a driver to select");
                    btnConfirmRide.setVisibility(View.GONE);
                    selectedDriver = null;
                });
            }

            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    tvRideStatus.setText("Error fetching drivers. Try again.");
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Task: Packages location, passenger, and driver metadata into a new RideRequest instance, publishes it to Supabase, and commands MainActivity to begin polling updates.
    // Input: None
    // Output: None
    private void confirmRide() {
        if (selectedDriver == null) {
            Toast.makeText(getContext(), "Please select a driver first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (passengerId == null) {
            Toast.makeText(getContext(), "Could not identify passenger. Please re-login.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmRide.setEnabled(false);
        tvRideStatus.setText("Sending ride request...");

        RideRequest request = new RideRequest(
                null,
                passengerId,
                selectedDriver.getId(),
                pickupLatLng.latitude,
                pickupLatLng.longitude,
                dropLatLng.latitude,
                dropLatLng.longitude,
                "requested"
        );

        RideRepo.createRide(request, new BaseRepo.RepoCallback<RideRequest>() {
            @Override
            public void onSuccess(RideRequest result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    // Save rideId for status tracking
                    SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
                    sp.edit().putString("activeRideId", result.getId()).apply();

                    tvRideStatus.setText("✅ Request sent! Waiting for driver to accept...");
                    btnConfirmRide.setVisibility(View.GONE);
                    btnFindDrivers.setEnabled(true);
                    Toast.makeText(getContext(), "Ride requested!", Toast.LENGTH_SHORT).show();
                    // Tell MainActivity to start polling
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).startPollingForRideAcceptance();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnConfirmRide.setEnabled(true);
                    tvRideStatus.setText("Failed to send request. Try again.");
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Task: Completely wipes all existing active user key records out of SharedPreferences and redirects the client device back onto LoginActivity.
    // Input: None
    // Output: None
    private void logout() {
        SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        sp.edit().clear().apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}