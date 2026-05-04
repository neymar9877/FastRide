package com.example.bigproject;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DriverRepo extends BaseRepo
{


    public void getAllUsers(RepoCallback<List<Driver>> callBack){
        String url = SUPABASE_URL + "/rest/v1/drivers";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Error fetching drivers", e.getMessage());
                callBack.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String json = response.body().string();
                    Type listType = new TypeToken<List<Driver>>() {
                    }.getType();
                    Log.d("Repository Users fetched", json);
                    List<Driver> users = gson.fromJson(json, listType);
                    callBack.onSuccess(users);
                    Log.d("Repository Users fetched", users.toString());
                } else{
                    Log.e("Repository Error fetching users", response.message() + response.code());
                    callBack.onError(new Exception("Error fetching users"));
                }
            }
        });
    }


    public void addUser(Driver newDriver, RepoCallback<Driver> callBack){
        String url = SUPABASE_URL + "/rest/v1/drivers";
        String json = gson.toJson(newDriver);
        Log.d("Repository", "Sending JSON: " + gson.toJson(newDriver));
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Repository", "Error adding user", e);
                callBack.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String jsonResponse = response.body().string();
                    Log.d("Repository", "Driver added successfully:" + jsonResponse);
                    Type listType = new TypeToken<List<Driver>>(){}.getType();
                    List<Driver> addedDrivers = gson.fromJson(jsonResponse, listType);
                    if (!addedDrivers.isEmpty()) {
                        Driver addedDriver = addedDrivers.get(0);
                        callBack.onSuccess(addedDriver);
                    }
                    else{
                        Log.e("Repository", "Error adding Driver" + response.message());
                        callBack.onError(new Exception("Error adding Driver" + response.code() + response.message()));
                    }
                }
                else {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    Log.e("Repository", "Insert failed → " + response.code() + " | " + response.message());
                    Log.e("Repository", "Error body: " + errorBody);
                    callBack.onError(new Exception("HTTP " + response.code() + ": " + errorBody));
                }
            }
        });
    }

    public void getDriversWithUsers(RepoCallback<List<DriverWithUser>> callBack) {
        // Step 1: Get all Drivers
        getAllUsers(new RepoCallback<List<Driver>>() {
            @Override
            public void onSuccess(List<Driver> drivers) {
                // Step 2: Get all Users from the users table
                String userUrl = SUPABASE_URL + "/rest/v1/users";
                Request userRequest = new Request.Builder()
                        .url(userUrl)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .build();

                client.newCall(userRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) { callBack.onError(e); }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String json = response.body().string();
                            Type listType = new TypeToken<List<User>>(){}.getType();
                            List<User> allUsers = gson.fromJson(json, listType);

                            // Step 3: Match them by ID
                            List<DriverWithUser> joinedList = new ArrayList<>();
                            for (Driver driver : drivers) {
                                DriverWithUser driverWithUser = new DriverWithUser();
                                driverWithUser.setId(driver.getId());
                                driverWithUser.setCurrentLocation(driver.getCurrentLocation());
                                driverWithUser.setStatus(driver.getStatus());

                                // Find the user with the same ID
                                for (User u : allUsers) {
                                    if (u.getId().equals(driver.getId())) {
                                        driverWithUser.setUsers(u);
                                        break;
                                    }
                                }
                                joinedList.add(driverWithUser);
                            }
                            callBack.onSuccess(joinedList);
                        } else {
                            callBack.onError(new Exception("Failed to fetch users for join"));
                        }
                    }
                });
            }

            @Override
            public void onError(Exception error) { callBack.onError(error); }
        });
    }


    // --- Update Driver Location ---
    public void updateDriverLocation(String driverId, double lat, double lng, RepoCallback<Boolean> callBack) {
        String url = SUPABASE_URL + "/rest/v1/drivers?id=eq." + driverId;

        String json = "{\"current_lat\":" + lat + ",\"current_lng\":" + lng + ",\"status\":\"available\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callBack.onError(e); }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) callBack.onSuccess(true);
                else callBack.onError(new Exception("Location update failed: " + response.code()));
            }
        });
    }

    // --- Get Driver's current lat/lng ---
    public void getDriverLocation(String driverId, RepoCallback<double[]> callBack) {
        String url = SUPABASE_URL + "/rest/v1/drivers?id=eq." + driverId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callBack.onError(e); }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) { callBack.onError(new Exception("HTTP " + response.code())); return; }
                String json = response.body().string();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<Driver>>(){}.getType();
                java.util.List<Driver> drivers = gson.fromJson(json, listType);
                if (drivers != null && !drivers.isEmpty()) {
                    Driver d = drivers.get(0);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callBack.onSuccess(new double[]{d.getCurrentLat(), d.getCurrentLng()})
                    );
                } else {
                    callBack.onError(new Exception("Driver not found"));
                }
            }
        });
    }

    // --- Get Available Drivers ---
    public void getAvailableDrivers(RepoCallback<List<DriverWithUser>> callBack) {
        String url = SUPABASE_URL + "/rest/v1/drivers?status=eq.available";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callBack.onError(e); }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) { callBack.onError(new Exception("Failed: " + response.code())); return; }
                String json = response.body().string();
                Type listType = new TypeToken<List<Driver>>(){}.getType();
                List<Driver> drivers = gson.fromJson(json, listType);

                // fetch users to join
                String userUrl = SUPABASE_URL + "/rest/v1/users";
                Request userRequest = new Request.Builder()
                        .url(userUrl)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .build();

                client.newCall(userRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) { callBack.onError(e); }
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) { callBack.onError(new Exception("Failed users")); return; }
                        String usersJson = response.body().string();
                        Type usersType = new TypeToken<List<User>>(){}.getType();
                        List<User> allUsers = gson.fromJson(usersJson, usersType);

                        List<DriverWithUser> result = new ArrayList<>();
                        for (Driver d : drivers) {
                            DriverWithUser dwu = new DriverWithUser();
                            dwu.setId(d.getId());
                            dwu.setCurrentLocation(d.getCurrentLocation());
                            dwu.setStatus(d.getStatus());
                            dwu.setCurrentLat(d.getCurrentLat());
                            dwu.setCurrentLng(d.getCurrentLng());
                            for (User u : allUsers) {
                                if (u.getId().equals(d.getId())) { dwu.setUsers(u); break; }
                            }
                            result.add(dwu);
                        }
                        callBack.onSuccess(result);
                    }
                });
            }
        });
    }
    public void updateDriver(Driver driver, RepoCallback<Driver> callBack) {
        String url = SUPABASE_URL + "/rest/v1/drivers?id=eq." + driver.getId();
        String json = gson.toJson(driver);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callBack.onError(e); }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResp = response.body().string();
                    Type listType = new TypeToken<List<Driver>>(){}.getType();
                    List<Driver> updated = gson.fromJson(jsonResp, listType);
                    callBack.onSuccess(updated.get(0));
                } else { callBack.onError(new Exception("Update failed")); }
            }
        });
    }

    // --- ADDED: Delete Method ---
    public void deleteDriver(Driver driver, RepoCallback<Boolean> callBack) {
        String url = SUPABASE_URL + "/rest/v1/drivers?id=eq." + driver.getId();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callBack.onError(e); }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) callBack.onSuccess(true);
                else callBack.onError(new Exception("Delete failed"));
            }
        });
    }
}
