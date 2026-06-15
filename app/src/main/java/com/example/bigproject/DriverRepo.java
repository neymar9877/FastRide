package com.example.bigproject;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Repository class for all operations on the 'drivers' table in Supabase.
 * Handles CRUD operations and joining driver data with user data.
 * Extends BaseRepo for shared HTTP client and credentials.
 */
public class DriverRepo extends BaseRepo {

    /**
     * Task: fetches all drivers from the Supabase 'drivers' table.
     * Input: callBack (RepoCallback<List<Driver>>)
     * Output: List<Driver> via callback, or Exception on failure
     */
    public void getAllUsers(RepoCallback<List<Driver>> callBack) {
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
                callBack.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Type listType = new TypeToken<List<Driver>>() {}.getType();
                    callBack.onSuccess(gson.fromJson(json, listType));
                } else {
                    callBack.onError(new Exception("Error fetching drivers"));
                }
            }
        });
    }

    /**
     * Task: inserts a new driver record into the Supabase 'drivers' table.
     * Should be called after addUser() in UserRepo with the same ID.
     * Input: newDriver (Driver), callBack (RepoCallback<Driver>)
     * Output: the inserted Driver with generated fields via callback
     */
    public void addUser(Driver newDriver, RepoCallback<Driver> callBack) {
        String url = SUPABASE_URL + "/rest/v1/drivers";
        String json = gson.toJson(newDriver);
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
                callBack.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Type listType = new TypeToken<List<Driver>>() {}.getType();
                    List<Driver> addedDrivers = gson.fromJson(jsonResponse, listType);
                    if (!addedDrivers.isEmpty()) callBack.onSuccess(addedDrivers.get(0));
                    else callBack.onError(new Exception("Error adding Driver"));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    callBack.onError(new Exception("HTTP " + response.code() + ": " + errorBody));
                }
            }
        });
    }

    /**
     * Task: fetches all drivers and joins each with their corresponding User data.
     * Performs two sequential requests: first to 'drivers', then to 'users'.
     * Matches by ID to create DriverWithUser objects.
     * Input: callBack (RepoCallback<List<DriverWithUser>>)
     * Output: List<DriverWithUser> with combined data via callback
     */
    public void getDriversWithUsers(RepoCallback<List<DriverWithUser>> callBack) {
        getAllUsers(new RepoCallback<List<Driver>>() {
            @Override
            public void onSuccess(List<Driver> drivers) {
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
                            Type listType = new TypeToken<List<User>>() {}.getType();
                            List<User> allUsers = gson.fromJson(json, listType);

                            List<DriverWithUser> joinedList = new ArrayList<>();
                            for (Driver driver : drivers) {
                                DriverWithUser driverWithUser = new DriverWithUser();
                                driverWithUser.setId(driver.getId());
                                driverWithUser.setCurrentLocation(driver.getCurrentLocation());
                                driverWithUser.setStatus(driver.getStatus());
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

    /**
     * Task: updates the GPS location and sets status to 'available' for a driver.
     * Called every step of the car animation in DriverMapFragment.
     * Input: driverId (String), lat (double), lng (double), callBack
     * Output: true via callback if updated successfully
     */
    public void updateDriverLocation(String driverId, double lat, double lng, String status, RepoCallback<Boolean> callBack) {
        String url = SUPABASE_URL + "/rest/v1/drivers?id=eq." + driverId;

        // 1. Put the values inside a safe Key-Value Map
        Map<String, Object> updateFields = new HashMap<>();
        updateFields.put("current_lat", lat);
        updateFields.put("current_lng", lng);
        updateFields.put("status", status);

        // 2. Let Gson serialize it flawlessly with correct decimal dots
        String json = gson.toJson(updateFields);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Log.d("CURRENT_PLACEEEEE", "coords: " + json);

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

    /**
     * Task: fetches the current GPS coordinates of a specific driver from Supabase.
     * Used by BlankFragment2 (passenger map) to show driver's real-time position.
     * Input: driverId (String), callBack (RepoCallback<double[]>)
     * Output: double[] {lat, lng} via callback, or Exception if driver not found
     */
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
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<Driver>>() {}.getType();
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

    /**
     * Task: fetches all drivers with status 'available' and joins with user data.
     * Used by BlankFragment3 to show the passenger a list of available drivers.
     * Input: callBack (RepoCallback<List<DriverWithUser>>)
     * Output: List<DriverWithUser> of available drivers with name and image via callback
     */
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
                Type listType = new TypeToken<List<Driver>>() {}.getType();
                List<Driver> drivers = gson.fromJson(json, listType);

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
                        Type usersType = new TypeToken<List<User>>() {}.getType();
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

    /**
     * Task: updates driver details (location, status) in Supabase.
     * Input: driver (Driver) — driver object with updated fields; callBack
     * Output: updated Driver via callback, or Exception on failure
     */
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
                    Type listType = new TypeToken<List<Driver>>() {}.getType();
                    List<Driver> updated = gson.fromJson(jsonResp, listType);
                    callBack.onSuccess(updated.get(0));
                } else {
                    callBack.onError(new Exception("Update failed"));
                }
            }
        });
    }

    /**
     * Task: deletes a driver record from the Supabase 'drivers' table.
     * Note: also call UserRepo.deleteUser() to fully remove the driver from the system.
     * Input: driver (Driver), callBack (RepoCallback<Boolean>)
     * Output: true via callback if deleted successfully
     */
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
