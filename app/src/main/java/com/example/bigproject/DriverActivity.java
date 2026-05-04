package com.example.bigproject;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.bigproject.driver_fragments.DriverHomeFragment;
import com.example.bigproject.driver_fragments.DriverMapFragment;
import com.example.bigproject.driver_fragments.DriverSettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DriverActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavBar;
    // debugging
    private boolean rideAccepted = true; // block map until true

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        initNavigationBar();
    }

    private void initNavigationBar() {
        bottomNavBar = findViewById(R.id.driver_bottom_navigation);

        // Driver ALWAYS starts at home
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.driver_fragment_container, new DriverHomeFragment())
                .commit();

        bottomNavBar.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_home) {
                selectedFragment = new DriverHomeFragment();
            }

            else if (item.getItemId() == R.id.navigation_map) {
                if (!rideAccepted) {
                    Toast.makeText(this,
                            "Accept a ride to open the map",
                            Toast.LENGTH_SHORT).show();
                    return false; // BLOCK map
                }
                selectedFragment = new DriverMapFragment();
            }

            else if (item.getItemId() == R.id.navigation_settings) {
                selectedFragment = new DriverSettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.driver_fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }

    // Call this when driver accepts a ride
    public void onRideAccepted() {
        rideAccepted = true;
        bottomNavBar.setSelectedItemId(R.id.navigation_map);
    }

    public void goToHomeFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.driver_fragment_container, new DriverHomeFragment())
                .commit();
        bottomNavBar.setSelectedItemId(R.id.navigation_home);
    }
}
