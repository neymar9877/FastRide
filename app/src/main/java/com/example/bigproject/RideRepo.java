package com.example.bigproject;

import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import android.os.Handler;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Repository class for all operations on the 'rides' table in Supabase.
 * All methods are static — no instantiation needed.
 * Handles creating rides, fetching by ID or driver, and updating status.
 */
public class RideRepo extends BaseRepo {

    /**
     * Task: creates a new ride request in Supabase when a passenger confirms a driver.
     * Input: ride (RideRequest) — the ride to create; callback (RepoCallback<RideRequest>)
     * Output: the created RideRequest with generated ID via callback
     */
    public static void createRide(RideRequest ride, RepoCallback<RideRequest> callback) {
        String url = SUPABASE_URL + "/rest/v1/rides";
        String json = gson.toJson(ride);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Type listType = new TypeToken<List<RideRequest>>() {}.getType();
                    List<RideRequest> rides = gson.fromJson(jsonResponse, listType);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(rides.get(0)));
                } else {
                    callback.onError(new Exception("Create ride failed"));
                }
            }
        });
    }

    /**
     * Task: fetches a single ride from Supabase by its unique ID.
     * Used by passenger to poll for status updates and by driver to load ride details.
     * Input: rideId (String), callback (RepoCallback<RideRequest>)
     * Output: RideRequest via callback if found, Exception if not found
     */
    public static void getRideById(String rideId, RepoCallback<RideRequest> callback) {
        String url = SUPABASE_URL + "/rest/v1/rides?id=eq." + rideId;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e); }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) { callback.onError(new Exception("HTTP " + response.code())); return; }
                String json = response.body().string();
                RideRequest[] rides = gson.fromJson(json, RideRequest[].class);
                if (rides.length == 0) { callback.onError(new Exception("Ride not found")); return; }
                callback.onSuccess(rides[0]);
            }
        });
    }

    /**
     * Task: fetches all pending ride requests assigned to a specific driver.
     * Called every 5 seconds by DriverHomeFragment to check for new requests.
     * Input: driverId (String), callback (RepoCallback<List<RideRequest>>)
     * Output: List<RideRequest> with status="requested" for the driver via callback
     */
    public static void getPendingRidesForDriver(String driverId, BaseRepo.RepoCallback<List<RideRequest>> callback) {
        String url = SUPABASE_URL + "/rest/v1/rides?driver_id=eq." + driverId + "&status=eq.requested";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e); }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) { callback.onError(new Exception("HTTP " + response.code())); return; }
                String json = response.body().string();
                Type listType = new TypeToken<List<RideRequest>>() {}.getType();
                List<RideRequest> rides = gson.fromJson(json, listType);
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(rides));
            }
        });
    }

    /**
     * Task: updates the status of a ride in Supabase.
     * Used throughout the ride lifecycle: accepted, on_the_way, finished, declined.
     * Input: rideId (String), status (String), callback (RepoCallback<Boolean>)
     * Output: true via callback if updated successfully, Exception on failure
     */
    public static void updateRideStatus(String rideId, String status, BaseRepo.RepoCallback<Boolean> callback) {
        String url = SUPABASE_URL + "/rest/v1/rides?id=eq." + rideId;
        String json = "{\"status\":\"" + status + "\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callback.onError(e); }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(true));
                else callback.onError(new Exception("Update failed: " + response.code()));
            }
        });
    }
}
