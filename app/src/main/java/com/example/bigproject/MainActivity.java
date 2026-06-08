package com.example.bigproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavBar;
    private Handler rideStatusHandler = new Handler(Looper.getMainLooper());
    private Runnable rideStatusRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        validateActiveRide(); // call this first — clears stale ride if it no longer exists
        initNavigationBar();
        startPollingForRideAcceptance();
    }


    // Task: Checks if the stored activeRideId still exists in Supabase and has a valid status.
// If the ride is finished, declined, or doesn't exist — clears it from SharedPreferences.
// Prevents the UI from being permanently locked after a crash or incomplete session.
    private void validateActiveRide() {
        SharedPreferences sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String rideId = sp.getString("activeRideId", null);
        if (rideId == null) return;

        RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
            @Override
            public void onSuccess(RideRequest ride) {
                String status = ride.getStatus();
                // If ride is finished, declined, or cancelled — clear it
                if ("finished".equals(status) || "declined".equals(status)
                        || "cancelled".equals(status)) {
                    sp.edit().remove("activeRideId").apply();
                }
            }
            @Override
            public void onError(Exception e) {
                // Ride ID not found in DB at all — safe to clear
                sp.edit().remove("activeRideId").apply();
            }
        });
    }


    // Task: Initializes the bottom navigation bar, sets the default fragment,
    // and blocks navigation away from the map during an active ride.
    private void initNavigationBar() {
        bottomNavBar = findViewById(R.id.bottom_navigation);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new BlankFragment3()).commit();

        bottomNavBar.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_map) {
                tryOpenMapFragment();
                return true;
            }

            if (item.getItemId() == R.id.nav_order) {

                // Block going back to order screen if ride is active
                SharedPreferences sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
                String rideId = sp.getString("activeRideId", null);
                if (rideId != null) {
                    Toast.makeText(this,
                            "Cannot leave map during an active ride",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new BlankFragment3()).commit();
            }

            return true;
        });
    }

    // Task: Polls Supabase every 3 seconds to detect when the driver accepts the ride,
    // then automatically switches the passenger to the map fragment.
    public void startPollingForRideAcceptance() {
        SharedPreferences sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String rideId = sp.getString("activeRideId", null);
        if (rideId == null) return;

        rideStatusRunnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
                String rideId = sp.getString("activeRideId", null);
                if (rideId == null) return;

                RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
                    @Override
                    public void onSuccess(RideRequest ride) {
                        runOnUiThread(() -> {
                            if ("accepted".equals(ride.getStatus())
                                    || "on_the_way".equals(ride.getStatus())) {
                                rideStatusHandler.removeCallbacks(rideStatusRunnable);
                                openMapFragment();
                                bottomNavBar.setSelectedItemId(R.id.nav_map);
                            } else {
                                rideStatusHandler.postDelayed(rideStatusRunnable, 3000);
                            }
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        rideStatusHandler.postDelayed(rideStatusRunnable, 3000);
                    }
                });
            }
        };
        rideStatusHandler.post(rideStatusRunnable);
    }

    // Task: Stops the polling runnable when the activity is destroyed to prevent memory leaks.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rideStatusHandler != null && rideStatusRunnable != null) {
            rideStatusHandler.removeCallbacks(rideStatusRunnable);
        }
    }

    // Task: Checks if the ride is in an accepted state before allowing
    // the passenger to manually tap the map tab.
    private void tryOpenMapFragment() {
        SharedPreferences sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String rideId = sp.getString("activeRideId", null);

        if (rideId == null) {
            Toast.makeText(this, "Order a ride first", Toast.LENGTH_SHORT).show();
            return;
        }

        RideRepo.getRideById(rideId, new BaseRepo.RepoCallback<RideRequest>() {
            @Override
            public void onSuccess(RideRequest ride) {
                runOnUiThread(() -> {
                    if ("accepted".equals(ride.getStatus())
                            || "on_the_way".equals(ride.getStatus())) {
                        openMapFragment();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Waiting for driver to accept",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Error checking ride", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Task: Replaces the fragment container with the map view (BlankFragment2).
    private void openMapFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BlankFragment2())
                .commit();
    }

    // Task: Switches the UI back to the ride ordering screen,
    // called by BlankFragment2 when the ride finishes.
    public void goToOrdersFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BlankFragment3())
                .commit();
        bottomNavBar.setSelectedItemId(R.id.nav_order);
    }
}