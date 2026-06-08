package com.example.bigproject;

import android.content.SharedPreferences;
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

    // Guard flag determining access privileges to navigation interfaces.
    // Setting this to true by default allows explicit feature overrides or testing.
    private boolean rideAccepted = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        // Sets up the initial navigation bar configurations and default fragment placements.
        initNavigationBar();
    }

    /**
     * Task: Initializes navigation widgets, mounts the starting dashboard view, and binds multi-view swap logic.
     * Input: None
     * Output: None
     */
    private void initNavigationBar() {
        bottomNavBar = findViewById(R.id.driver_bottom_navigation);

        // Driver ALWAYS starts at home dashboard by committing an initial FragmentTransaction
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.driver_fragment_container, new DriverHomeFragment())
                .commit();

        // Sets up a listener to monitor tab changes made by the user
        bottomNavBar.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_home) {

                // check if the driver is driving - if so not letting him change fragments
                SharedPreferences sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
                String rideId = sp.getString("activeRideId", null);
                if (rideId != null) {
                    Toast.makeText(this,
                            "Cannot leave map during an active ride",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                selectedFragment = new DriverHomeFragment();
            }

            else if (item.getItemId() == R.id.navigation_map) {
                if (!rideAccepted) {
                    Toast.makeText(this, "Accept a ride to open the map", Toast.LENGTH_SHORT).show();
                    return false;
                }
                selectedFragment = new DriverMapFragment();
            }

            else if (item.getItemId() == R.id.navigation_settings) {
                // block also settings fragment
                SharedPreferences sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
                String rideId = sp.getString("activeRideId", null);
                if (rideId != null) {
                    Toast.makeText(this,
                            "Cannot leave map during an active ride",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                selectedFragment = new DriverSettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.driver_fragment_container, selectedFragment).commit();
            }
            return true;
        });
    }

    /**
     * Task: Callback routine triggered by DriverHomeFragment when an order is swiped right.
     * It updates state flags and forces a tab transition over to the interactive navigation map.
     * Input: None
     * Output: None
     */
    public void onRideAccepted() {
        rideAccepted = true;
        bottomNavBar.setSelectedItemId(R.id.navigation_map);
    }

    /**
     * Task: Callback routine triggered by DriverMapFragment upon completing a customer drop-off.
     * It resets the layout and redirects back to the pending job list.
     * Input: None
     * Output: None
     */
    public void goToHomeFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.driver_fragment_container, new DriverHomeFragment())
                .commit();
        bottomNavBar.setSelectedItemId(R.id.navigation_home);
    }
}