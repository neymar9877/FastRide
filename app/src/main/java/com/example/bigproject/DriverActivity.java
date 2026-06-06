package com.example.bigproject;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.bigproject.driver_fragments.DriverHomeFragment;
import com.example.bigproject.driver_fragments.DriverMapFragment;
import com.example.bigproject.driver_fragments.DriverSettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Class Meaning and Contribution to the Application:
 * * 1. Root Container for Driver Workspace:
 * This Activity acts as the core controller and UI host for the entire driver experience.
 * Instead of spinning up multiple resource-heavy activities, it stays active and uses
 * a single container (`R.id.driver_fragment_container`) to seamlessly swap layout chunks.
 * * 2. Navigation State Management & Gatekeeping:
 * It coordinates the state transitions of a trip. By managing flags like `rideAccepted`,
 * it prevents illegal workflow transitions—ensuring a driver cannot access navigation tools
 * unless a passenger's request has been officially accepted.
 * * 3. Bidirectional Fragment Communication:
 * It acts as a central communication bridge. Other child sub-fragments (like Home or Map)
 * cast runtime status events back up to this activity, allowing the host to dynamically
 * alter structural properties like active bottom navigation selection.
 */
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
                selectedFragment = new DriverHomeFragment();
            }

            else if (item.getItemId() == R.id.navigation_map) {
                // Security Check: Block map access if the driver does not have an active ride
                if (!rideAccepted) {
                    Toast.makeText(this,
                            "Accept a ride to open the map",
                            Toast.LENGTH_SHORT).show();
                    return false; // Returns false to cancel the navigation selection highlight
                }
                selectedFragment = new DriverMapFragment();
            }

            else if (item.getItemId() == R.id.navigation_settings) {
                selectedFragment = new DriverSettingsFragment();
            }

            // Executes the asynchronous screen transition if a matching route was resolved
            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.driver_fragment_container, selectedFragment)
                        .commit();
            }

            return true; // Returns true to acknowledge and visually render the item selection
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