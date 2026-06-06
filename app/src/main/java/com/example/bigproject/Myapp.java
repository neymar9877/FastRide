package com.example.bigproject;

import android.app.Application;
import org.osmdroid.config.Configuration;

/**
 * Class Meaning and Contribution to the Application:
 * * 1. Global Initialization Point (Application Context):
 * This class extends the Android Application class. The onCreate() method runs
 * exactly once when the application process starts, even before any Activity
 * or Fragment is loaded. This makes it the ideal place for global configurations.
 * * 2. Critical Configuration for OpenStreetMap (osmdroid):
 * The line inside onCreate() registers a unique User-Agent identifier with the
 * OpenStreetMap servers using the app's package name ("com.example.bigproject").
 * * 3. Preventing Tile Loading Blocks:
 * OpenStreetMap policies require all tile requests to identify themselves. Without
 * setting a proper User-Agent, the map servers might identify the requests as
 * anonymous or automated bots and block them. This configuration ensures that
 * map tiles render properly and reliably across the Driver Map and Passenger views.
 */
public class Myapp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Sets the package name as the User-Agent to comply with OSM server policies
        // and allow smooth map tile downloading.
        Configuration.getInstance().setUserAgentValue("com.example.bigproject");
    }
}