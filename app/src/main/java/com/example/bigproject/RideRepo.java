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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class RideRepo extends BaseRepo
{


    // 1️⃣ Passenger creates a ride request
    public static void createRide(RideRequest ride, RepoCallback<RideRequest> callback) {
        String url = SUPABASE_URL + "/rest/v1/rides";

        String json = gson.toJson(ride);
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );
        Log.d("aloo", "enter the func");


        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();
        Log.d("aloo", "after the request design");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("aloo", "on fail!!", e);
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("RideRepoCheckLog", "Create ride response = " + jsonResponse);

                    Type listType = new TypeToken<List<RideRequest>>(){}.getType();
                    List<RideRequest> rides = gson.fromJson(jsonResponse, listType);
                    RideRequest createdRide = rides.get(0);

                    // go back to the main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(createdRide);
                    });

                } else {
                    callback.onError(new Exception("Create ride failed"));
                }
            }
        });
    }

    public static void getRideById(String rideId, RepoCallback<RideRequest> callback){
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception("HTTP " + response.code()));
                    return;
                }
                String json = response.body().string();
                RideRequest[] rides = gson.fromJson(json, RideRequest[].class);

                if (rides.length == 0) {
                    callback.onError(new Exception("Ride not found"));
                    return;
                }
                callback.onSuccess(rides[0]);
            }

        });
    }




    // Driver sees pending requests for him
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
                Type listType = new com.google.gson.reflect.TypeToken<List<RideRequest>>(){}.getType();
                List<RideRequest> rides = gson.fromJson(json, listType);
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(rides));
            }
        });
    }

    // Update ride status (accepted / declined / on_the_way / finished)
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

    // Driver sees pending requests (old commented code)
//        public void getPendingRidesForDriver(
//                String driverId,
//                RideCallback<List<RideRequest>> callback
//        ) {
//            String url = SUPABASE_URL +
//                    "/rest/v1/rides?driver_id=eq." + driverId +
//                    "&status=eq.requested";
//
//            Request request = new Request.Builder()
//                    .url(url)
//                    .addHeader("apikey", SUPABASE_KEY)
//                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                    .addHeader("Accept", "application/json")
//                    .build();
//
//            client.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                    callback.onError(e);
//                }
//
//                @Override
//                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                    if (response.isSuccessful()) {
//                        String json = response.body().string();
//                        Type listType = new TypeToken<List<RideRequest>>(){}.getType();
//                        List<RideRequest> rides = gson.fromJson(json, listType);
//                        callback.onSuccess(rides);
//                    } else {
//                        callback.onError(new Exception("Fetch failed"));
//                    }
//                }
//            });
//        }
//
//        // Driver accepts ride
//        public void acceptRide(String rideId, RideCallback<Void> callback) {
//            String url = SUPABASE_URL + "/rest/v1/rides?id=eq." + rideId;
//
//            RequestBody body = RequestBody.create(
//                    "{\"status\":\"accepted\"}",
//                    MediaType.parse("application/json")
//            );
//
//            Request request = new Request.Builder()
//                    .url(url)
//                    .patch(body)
//                    .addHeader("apikey", SUPABASE_KEY)
//                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
//                    .addHeader("Content-Type", "application/json")
//                    .build();
//
//            client.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                    callback.onError(e);
//                }
//
//                @Override
//                public void onResponse(@NonNull Call call, @NonNull Response response) {
//                    callback.onSuccess(null);
//                }
//            });
//        }
//
}

