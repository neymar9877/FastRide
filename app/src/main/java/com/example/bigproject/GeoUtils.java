package com.example.bigproject;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/**
 * Utility class for geographic operations.
 * Provides address-to-coordinates conversion using Android's Geocoder.
 * All methods are static — no instantiation needed.
 * IMPORTANT: must be called on a background thread, not the main thread.
 */
public class GeoUtils {

    // Borders - within the borders of Israel
    private static final double ISRAEL_MIN_LAT = 29.4;
    private static final double ISRAEL_MAX_LAT = 33.4;
    private static final double ISRAEL_MIN_LNG = 34.2;
    private static final double ISRAEL_MAX_LNG = 35.9;

    /**
     * Simple data class to hold a latitude/longitude pair.
     */
    public static class LatLng {
        public double latitude;
        public double longitude;

        /**
         * Task: constructs a LatLng coordinate pair.
         * Input: lat (double), lng (double)
         * Output: LatLng instance
         */
        public LatLng(double lat, double lng) {
            this.latitude = lat;
            this.longitude = lng;
        }
    }

    /**
     * Task: converts a text address into GPS coordinates using Android Geocoder.
     * Must be called on a background thread to avoid ANR crashes.
     * Input: context (Context), addressString (String) — the address to geocode
     * Output: LatLng with latitude/longitude, or null if address not found or error occurs
     */
    public static LatLng getLatLngFromAddress(Context context, String addressString) {
        if (context == null || addressString == null || addressString.trim().isEmpty()) {
            return null;
        }

        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> results = geocoder.getFromLocationName(addressString, 1);

            if (results != null && !results.isEmpty()) {
                Address address = results.get(0);
                double lat = address.getLatitude();
                double lng = address.getLongitude();
// ===== Check: Is the address within Israel? =====
// If not – return null, and the user will see an error message
                    if (lat < ISRAEL_MIN_LAT || lat > ISRAEL_MAX_LAT ||
                        lng < ISRAEL_MIN_LNG || lng > ISRAEL_MAX_LNG) {
                    Log.w("GeoUtils", "Address out of Israel bounds: " + lat + ", " + lng);
                    return null; // BlankFragment3 handels null and shows Toast
                }

                return new LatLng(lat, lng);
            }

        } catch (Exception e) {
            Log.e("GeoUtils", "Geocoding failed: " + e.getMessage());
        }

        return null;
    }
}
