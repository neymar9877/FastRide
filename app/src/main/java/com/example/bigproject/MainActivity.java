package com.example.bigproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bigproject.RideAdapter;
import com.example.bigproject.Driver;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.UUID;
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

        initNavigationBar();
        startPollingForRideAcceptance();
    }

    // Task: Starts a background loop that repeatedly checks the database for updates on the active ride's status. If accepted, it moves the user to the map.
    // Input: None
    // Output: None
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
                            if ("accepted".equals(ride.getStatus()) || "on_the_way".equals(ride.getStatus())) {
                                // Stop polling
                                rideStatusHandler.removeCallbacks(rideStatusRunnable);
                                // Auto switch to map!
                                openMapFragment();
                                bottomNavBar.setSelectedItemId(R.id.nav_map);
                            } else {
                                // Keep polling
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

    // Task: Cleans up the background Handler and stops the polling runnable when the activity is destroyed to prevent memory leaks.
    // Input: None
    // Output: None
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rideStatusHandler != null && rideStatusRunnable != null) {
            rideStatusHandler.removeCallbacks(rideStatusRunnable);
        }
    }

    // Task: Initializes the bottom navigation bar, sets up the default landing fragment, and listens for navigation item selection.
    // Input: None
    // Output: None
    private void initNavigationBar() {
        bottomNavBar = findViewById(R.id.bottom_navigation);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new BlankFragment3()).commit();
        bottomNavBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                if (item.getItemId() == R.id.nav_map) {
                    tryOpenMapFragment();
                    return true; // stop default navigation
                }
                if (item.getItemId() == R.id.nav_order)
                    selectedFragment = new BlankFragment3();
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                }
                return true;
            }
        });
    }

    // Task: Programmatically forces the navigation bar and the UI fragment container to switch back to the ride ordering screen.
    // Input: None
    // Output: None
    public void goToOrdersFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BlankFragment3())
                .commit();
        bottomNavBar.setSelectedItemId(R.id.nav_order);
    }

    // ===================== RIDE ACCESS LOGIC =====================

    // Task: Validates if the user has an active, accepted ride before allowing them to manually switch over to the map view.
    // Input: None
    // Output: None
    private void tryOpenMapFragment() {
        SharedPreferences sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String rideId = sp.getString("activeRideId", null);

        if (rideId == null) {
            Toast.makeText(this, "Order a ride first", Toast.LENGTH_SHORT).show();
            return;
        }

        RideRepo.getRideById(rideId, new RideRepo.RepoCallback<RideRequest>() {
            @Override
            public void onSuccess(RideRequest ride) {
                runOnUiThread(() -> {
                    if ("accepted".equals(ride.getStatus())) {
                        bottomNavBar.getMenu()
                                .findItem(R.id.nav_map)
                                .setEnabled(true);
                        openMapFragment();
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                "Waiting for driver to accept",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error checking ride", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // Task: Replaces the current fragment container view with the map fragment (BlankFragment2).
    // Input: None
    // Output: None
    private void openMapFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BlankFragment2())
                .commit();
    }


}