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

/**
 * Repository class for all operations on the 'users' table in Supabase.
 * Handles CRUD operations and user lookup by various criteria.
 * Extends BaseRepo for shared HTTP client and credentials.
 */
public class UserRepo extends BaseRepo {

    /**
     * Task: fetches all users from the Supabase 'users' table.
     * Input: callBack (RepoCallback<List<User>>) — returns list on success
     * Output: List<User> via callback, or Exception on failure
     */
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
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> users = gson.fromJson(json, listType);
                    callBack.onSuccess(users);
                } else {
                    callBack.onError(new Exception("Error fetching users"));
                }
            }
        });
    }

    /**
     * Task: updates an existing user's details in Supabase.
     * Input: user (User) — user object with updated fields; callback
     * Output: updated User via callback, or Exception on failure
     */
    public void updateUser(User user, RepoCallback<User> callback) {
        String url = SUPABASE_URL + "/rest/v1/users?id=eq." + user.getId();
        String json = gson.toJson(user);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

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
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) callback.onSuccess(user);
                else callback.onError(new Exception("Update failed: " + response.code()));
            }
        });
    }

    /**
     * Task: checks if a user with the given username exists in the system.
     * Input: userName (String), callback (RepoCallback<User>)
     * Output: the matching User via callback if found, null if not found
     */
    public void CheckUserExist(String userName, RepoCallback<User> callback) {
        getAllUsers(new RepoCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                for (User user : result) {
                    if (user.getUserName().equals(userName)) {
                        callback.onSuccess(user);
                        return;
                    }
                }
                callback.onSuccess(null);
            }

            @Override
            public void onError(Exception error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Task: adds a new user to the Supabase 'users' table.
     * Input: newUser (User) — user object to insert; callBack
     * Output: the inserted User (with generated id) via callback
     */
    public void addUser(User newUser, UserRepo.RepoCallback<User> callBack) {
        String url = SUPABASE_URL + "/rest/v1/users";
        String json = gson.toJson(newUser);
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
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> addedUsers = gson.fromJson(jsonResponse, listType);
                    if (!addedUsers.isEmpty()) callBack.onSuccess(addedUsers.get(0));
                    else callBack.onError(new Exception("Error adding User"));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    callBack.onError(new Exception("HTTP " + response.code() + ": " + errorBody));
                }
            }
        });
    }

    /**
     * Task: fetches a single user from Supabase by their unique ID.
     * Input: userId (String), callback (RepoCallback<User>)
     * Output: User via callback if found, Exception if not found
     */
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
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> users = gson.fromJson(json, listType);
                    if (!users.isEmpty()) callback.onSuccess(users.get(0));
                    else callback.onError(new Exception("User not found"));
                } else {
                    callback.onError(new Exception("Error fetching user"));
                }
            }
        });
    }

    /**
     * Task: fetches all users with a specific role from Supabase.
     * Input: role (String) — e.g. "driver", "passenger"; callBack
     * Output: List<User> with matching role via callback
     */
    public void getUsersByRole(String role, RepoCallback<List<User>> callBack) {
        String url = SUPABASE_URL + "/rest/v1/users?role=eq." + role;
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
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    callBack.onSuccess(gson.fromJson(json, listType));
                } else {
                    callBack.onError(new Exception("Failed to fetch users by role: " + response.code()));
                }
            }
        });
    }

    /**
     * Task: deletes a user from the Supabase 'users' table by ID.
     * Input: userId (String), callBack (RepoCallback<Boolean>)
     * Output: true via callback if deleted successfully, Exception on failure
     */
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) { callBack.onError(e); }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) callBack.onSuccess(true);
                else callBack.onError(new Exception("Delete failed: " + response.code()));
            }
        });
    }
}
