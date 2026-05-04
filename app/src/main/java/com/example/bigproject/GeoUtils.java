package com.example.bigproject;


import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.util.List;
import java.util.Locale;

public class GeoUtils {

    public static class LatLng {
        public double latitude;
        public double longitude;

        public LatLng(double lat, double lng) {
            this.latitude = lat;
            this.longitude = lng;
        }
    }


    /**
     * Convert an address string into coordinates using Android Geocoder.
     *
     * return LatLng OR null if not found
     **/

    public static LatLng getLatLngFromAddress(Context context, String addressString) {
        if (context == null || addressString == null || addressString.trim().isEmpty()) {
            return null;
        }

        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> results = geocoder.getFromLocationName(addressString, 1);

            if (results != null && !results.isEmpty()) {
                Address address = results.get(0);
                return new LatLng(address.getLatitude(), address.getLongitude());
            }

        } catch (Exception e) {
            Log.e("GeoUtils", "Geocoding failed: " + e.getMessage());
        }

        return null;
    }
}
