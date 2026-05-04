package com.example.bigproject;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class UserRepo extends BaseRepo
{
    public static void getAllUsers(RepoCallback<List<User>> callBack) {
        String url = SUPABASE_URL + "/rest/v1/users";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Error fetching users", e.getMessage());
                callBack.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Type listType = new TypeToken<List<User>>() {
                    }.getType();
                    Log.d("Repository Users fetched", json);
                    List<User> users = gson.fromJson(json, listType);
                    callBack.onSuccess(users);
                    Log.d("Repository Users fetched", users.toString());
                } else {
                    Log.e("Repository Error fetching users", response.message() + response.code());
                    callBack.onError(new Exception("Error fetching users"));
                }
            }
        });
    }

    public void updateUser(User user, RepoCallback<User> callback) {
        String url = SUPABASE_URL + "/rest/v1/users?id=eq." + user.getId();
        String json = gson.toJson(user);

        RequestBody body =
                RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("UserRepo", "Update failed", e);
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {

                if (response.isSuccessful()) {
                    callback.onSuccess(user);
                } else {
                    callback.onError(
                            new Exception("Update failed: " + response.code())
                    );
                }
            }
        });
    }
        // by userName
    public void CheckUserExist(String userName, RepoCallback<User> callback){
        getAllUsers(new RepoCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                for (User user : result) {
                    if (user.getUserName().equals(userName)) {

                        // user exists
                        callback.onSuccess(user);
                        return;
                    }
                }

                // no match found
                callback.onSuccess(null);
            }

            @Override
            public void onError(Exception error) {
                    callback.onError(error);
            }
        });
    }


    public void addUser(User newUser, UserRepo.RepoCallback<User> callBack) {
        String url = SUPABASE_URL + "/rest/v1/users";
        String json = gson.toJson(newUser);
        Log.d("Repository", "Sending JSON: " + gson.toJson(newUser));
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
                    Log.d("Repository", "User added successfully:" + jsonResponse);
                    Type listType = new TypeToken<List<User>>(){}.getType();
                    List<User> addedUsers = gson.fromJson(jsonResponse, listType);
                    if (!addedUsers.isEmpty()) {
                        User addedUser = addedUsers.get(0);
                        callBack.onSuccess(addedUser);
                    }
                    else{
                        Log.e("Repository", "Error adding User" + response.message());
                        callBack.onError(new Exception("Error adding User" + response.code() + response.message()));
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

    public void getUserById(String userId, RepoCallback<User> callback) {

        String url = SUPABASE_URL + "/rest/v1/users?id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
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

                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Type listType = new TypeToken<List<User>>(){}.getType();
                    List<User> users = gson.fromJson(json, listType);

                    if (!users.isEmpty()) {
                        callback.onSuccess(users.get(0));
                    } else {
                        callback.onError(new Exception("User not found"));
                    }

                } else {
                    callback.onError(new Exception("Error fetching user"));
                }
            }
        });
    }

    // 1. Fetch only users with a specific role (drivers)
    public void getUsersByRole(String role, RepoCallback<List<User>> callBack) {
        // This query filters the users table directly: /rest/v1/users?role=eq.driver
        String url = SUPABASE_URL + "/rest/v1/users?role=eq." + role;

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
                    Type listType = new TypeToken<List<User>>(){}.getType();
                    List<User> users = gson.fromJson(json, listType);
                    callBack.onSuccess(users);
                } else {
                    callBack.onError(new Exception("Failed to fetch users by role: " + response.code()));
                }
            }
        });
    }

    // 2. Delete a user (driver)
    public void deleteUser(String userId, RepoCallback<Boolean> callBack) {
        String url = SUPABASE_URL + "/rest/v1/users?id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callBack.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    callBack.onSuccess(true);
                } else {
                    callBack.onError(new Exception("Delete failed: " + response.code()));
                }
            }
        });
    }

}
