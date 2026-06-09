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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bigproject.BaseRepo;
import com.example.bigproject.DriverActivity;
import com.example.bigproject.DriverRepo;
import com.example.bigproject.R;
import com.example.bigproject.RideRepo;
import com.example.bigproject.RideRequest;
import com.example.bigproject.RideRequestAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class DriverHomeFragment extends Fragment {

    private RecyclerView recyclerRequests;
    private RideRequestAdapter adapter;
    private ArrayList<RideRequest> requestsList;
    private TextView tvNoRequests;
    private TextView tvSwipeHint;

    private FusedLocationProviderClient fusedLocationClient;
    private DriverRepo driverRepo;
    private String driverId;

    private Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;

    private com.google.android.gms.location.LocationCallback homeLocationCallback;

    public DriverHomeFragment() {}

    // Task: Inflates the fragment layout representing the driver's job request dashboard.
    // Input: inflater (LayoutInflater), container (ViewGroup), savedInstanceState (Bundle)
    // Output: View
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driver_home, container, false);
    }

    // Task: Binds XML elements, checks/requests location permissions, triggers the polling loop, and hooks swipe listeners (Left to Decline, Right to Accept) onto the list view.
    // Input: view (View), savedInstanceState (Bundle)
    // Output: None
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerRequests = view.findViewById(R.id.recyclerRideRequests);
        tvNoRequests = view.findViewById(R.id.tvNoRequests);
        tvSwipeHint = view.findViewById(R.id.tvSwipeHint);

        driverRepo = new DriverRepo();
        requestsList = new ArrayList<>();

        SharedPreferences sp = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        String savedId = sp.getString("userId", null);

        if (savedId != null) {
            driverId = savedId;

            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 101);
            } else {
                saveLocationToSupabase();
            }
            startPolling();
        }

        adapter = new RideRequestAdapter(requestsList, new RideRequestAdapter.OnRideActionListener() {
            @Override
            public void onAccept(RideRequest ride, int position) { acceptRide(ride, position); }
            @Override
            public void onDecline(RideRequest ride, int position) { declineRide(ride, position); }
        });

        recyclerRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerRequests.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                RideRequest ride = requestsList.get(position);
                if (direction == ItemTouchHelper.RIGHT) acceptRide(ride, position);
                else declineRide(ride, position);
            }
        }).attachToRecyclerView(recyclerRequests);
    }

    // Task: Catches the asynchronous OS response for location requests; executes the database location update if granted, otherwise informs the user.
    // Input: requestCode (int), permissions (String[]), grantResults (int[])
    // Output: None
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveLocationToSupabase();
            } else {
                Toast.makeText(getContext(),
                        "Location permission is required to show your position to passengers.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // Task: Uses the Google FusedLocationProviderClient to fetch high-accuracy single GPS points and updates the driver's online coordinates in Supabase.
    // Input: None
    // Output: None
    private void saveLocationToSupabase() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        com.google.android.gms.location.LocationRequest locationRequest =
                new com.google.android.gms.location.LocationRequest.Builder(0)
                        .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                        .setMaxUpdates(1)
                        .build();

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        // Save to field so we can unregister it in onDestroyView
        homeLocationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {

                // Guard: fragment may have detached by the time GPS fires — stop immediately
                if (!isAdded() || getContext() == null) {
                    if (fusedLocationClient != null && homeLocationCallback != null)
                        fusedLocationClient.removeLocationUpdates(homeLocationCallback);
                    return;
                }

                if (locationResult == null) return;
                android.location.Location location = locationResult.getLastLocation();
                if (location != null && driverId != null) {
                    driverRepo.updateDriverLocation(driverId,
                            location.getLatitude(),
                            location.getLongitude(),
                            "available",
                            new BaseRepo.RepoCallback<Boolean>() {
                                @Override public void onSuccess(Boolean result) {
                                    Log.d("DriverHome", "Location saved: "
                                            + location.getLatitude() + ", "
                                            + location.getLongitude());
                                }
                                @Override public void onError(Exception error) {
                                    Log.e("DriverHome", "Location save failed: "
                                            + error.getMessage());
                                }
                            });
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                homeLocationCallback,
                Looper.getMainLooper()
        );
    }

    // Task: Begins a repeating loop using a Handler that periodically runs a fetch commands queue every 5000 milliseconds.
    // Input: None
    // Output: None
    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                fetchRideRequests();
                pollHandler.postDelayed(this, 5000);
            }
        };
        pollHandler.post(pollRunnable);
    }

    // Task: Queries the ride repository for open, matching orders designated for this driver ID and alters UI warning labels based on results availability.
    // Input: None
    // Output: None
    private void fetchRideRequests() {
        if (driverId == null) return;
        RideRepo.getPendingRidesForDriver(driverId, new BaseRepo.RepoCallback<List<RideRequest>>() {
            @Override
            public void onSuccess(List<RideRequest> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    requestsList.clear();
                    requestsList.addAll(result);
                    adapter.notifyDataSetChanged();
                    if (requestsList.isEmpty()) {
                        tvNoRequests.setVisibility(View.VISIBLE);
                        tvSwipeHint.setVisibility(View.GONE);
                        recyclerRequests.setVisibility(View.GONE);
                    } else {
                        tvNoRequests.setVisibility(View.GONE);
                        tvSwipeHint.setVisibility(View.VISIBLE);
                        recyclerRequests.setVisibility(View.VISIBLE);
                    }
                });
            }
            @Override
            public void onError(Exception error) {
                Log.e("DriverHome", "Fetch error: " + error.getMessage());
            }
        });
    }

    // Task: Submits an asynchronous network request to set a ride's status to "accepted", saves the session ID locally, and signals DriverActivity to launch navigation.
    // Input: ride (RideRequest), position (int)
    // Output: None
    private void acceptRide(RideRequest ride, int position) {
        RideRepo.updateRideStatus(ride.getId(), "accepted", new BaseRepo.RepoCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    SharedPreferences sp = requireContext()
                            .getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
                    sp.edit().putString("activeRideId", ride.getId()).apply();

                    requestsList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Ride accepted! Opening map...",
                            Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof DriverActivity) {
                        ((DriverActivity) getActivity()).onRideAccepted();
                    }
                });
            }
            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to accept", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
            }
        });
    }

    // Task: Updates the specific ride ticket's state to "declined" inside the repository layer and cleans it from the active screen array.
    // Input: ride (RideRequest), position (int)
    // Output: None
    private void declineRide(RideRequest ride, int position) {
        RideRepo.updateRideStatus(ride.getId(), "declined", new BaseRepo.RepoCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    requestsList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Ride declined", Toast.LENGTH_SHORT).show();
                    // הסטטוס "declined" נשמר ב-Supabase –
                    // BlankFragment3 של הנוסע יזהה אותו בפולינג הבא
                });
            }
            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        adapter.notifyItemChanged(position));
            }
        });
    }

    // Task: Tears down and unregisters looping runnables when the fragment transitions out of focus to preserve processing loops.
    // Input: None
    // Output: None
    @Override
    public void onPause() {
        super.onPause();
        if (pollHandler != null && pollRunnable != null)
            pollHandler.removeCallbacks(pollRunnable);
    }

    // Task: Automatic recovery callback that re-instantiates data-refresh polling tasks when the context goes active.
    // Input: None
    // Output: None
    @Override
    public void onResume() {
        super.onResume();
        if (driverId != null) startPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister GPS callback so it never fires into a detached fragment
        if (fusedLocationClient != null && homeLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(homeLocationCallback);
            homeLocationCallback = null;
        }
        // Stop polling as well
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }
}