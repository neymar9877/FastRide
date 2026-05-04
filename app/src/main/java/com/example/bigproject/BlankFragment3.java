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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank3, container, false);
    }

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

    private void loadPassengerId() {
        SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);

        // Get the saved userId directly — no network call needed
        String savedId = sp.getString("userId", null);
        if (savedId != null) {
            passengerId = savedId;
        }
    }

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
                    Toast.makeText(getContext(), "Could not find address. Try again.", Toast.LENGTH_SHORT).show();
                    tvRideStatus.setText("");
                    return;
                }

                // Now fetch available drivers
                fetchAvailableDrivers();
            });
        }).start();
    }

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
                            float[] distResult = new float[1];
                            Log.d("COORD_CHECK", "Pickup Lat/Lng: " + pickupLatLng.latitude + ", " + pickupLatLng.longitude);
                            Log.d("COORD_CHECK", "Driver Lat/Lng: " + driver.getCurrentLat() + ", " + driver.getCurrentLng());
                            Location.distanceBetween(
                                    pickupLatLng.latitude, pickupLatLng.longitude,
                                    driver.getCurrentLat(), driver.getCurrentLng(),
                                    distResult
                            );

                            double distanceInKm = distResult[0] / 1000.0;
                            driver.setDistanceKm(distanceInKm);

                            // LOG INSIDE THE LOOP so you see every driver
                            Log.d("DISTANCE_CHECK", "Driver: " + driver.toDriver().getId() + " | Dist: " + distanceInKm + " km");
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

    private void logout() {
        SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        sp.edit().clear().apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
