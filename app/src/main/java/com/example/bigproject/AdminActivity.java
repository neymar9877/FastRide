package com.example.bigproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminActivity extends AppCompatActivity {
    private RecyclerView recyclerDrivers;
    private RideAdapter adapter;
    private ArrayList<DriverWithUser> driversList;
    private FloatingActionButton fabAdd;
    private DriverRepo driverRepo;
    private UserRepo userRepo;
    private Button btnLogOut;
    public static final String DEFAULT_IMAGE_URL = "https://img.freepik.com/premium-vector/default-male-user-profile-icon-vector-illustration_276184-168.jpg";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);


        userRepo = new UserRepo();
        recyclerDrivers = findViewById(R.id.recyclerDrivers);
        fabAdd = findViewById(R.id.fabAction);
        btnLogOut = findViewById(R.id.btnLogout);
        // set a listener for the btnLogout
        btnLogOut.setOnClickListener(v -> {
            // SAME prefs as LoginActivity
            SharedPreferences sp =
                    getSharedPreferences("myPrefs", MODE_PRIVATE);

            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("remember", false);
            editor.remove("userName");
            editor.remove("password");
            editor.apply();

            // Go back to login & clear back stack
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        driversList = new ArrayList<>();
        adapter = new RideAdapter(driversList, new RideAdapter.OnItemActionListener() {
            @Override
            public void onEdit(DriverWithUser driver, int position) {

            }

            @Override
            public void onDelete(DriverWithUser driver, int position) {

            }

            @Override
            public void onClick(DriverWithUser driver, int position) {
                Toast.makeText(AdminActivity.this, "Clicked: " + driver.getId(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerDrivers.setLayoutManager(new LinearLayoutManager(AdminActivity.this));
        recyclerDrivers.setAdapter(adapter);

        driverRepo = new DriverRepo();
        getDrivers();

        // FAB → add new ride
        fabAdd.setOnClickListener(v -> showAddDriverDialog());

        // swipe actions
        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(0, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                DriverWithUser driver = driversList.get(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    // update
                    showUpdateDriverDialog(driver, position);
                }
                else if (direction == ItemTouchHelper.LEFT) {
                    // Delete
                    deleteDriver(driver, position);
                }
            }
        }).attachToRecyclerView(recyclerDrivers);

    }

    // Task: Asynchronously fetches all drivers along with their user profile details from the repository and populates the RecyclerView.
    // Input: None
    // Output: None
    public void getDrivers(){
        DriverRepo repo = new DriverRepo();
        repo.getDriversWithUsers(new BaseRepo.RepoCallback<List<DriverWithUser>>() {
            @Override
            public void onSuccess(List<DriverWithUser> result) {
                runOnUiThread(() -> {
                    driversList.clear();
                    driversList.addAll(result);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                Log.e("Error fetching drivers", error.getMessage());

                runOnUiThread(() ->
                        Toast.makeText(
                                AdminActivity.this,
                                "Error: " + error.getMessage(),   // show real reason
                                Toast.LENGTH_LONG
                        ).show()
                );
                Log.d("Alooooon", "Error: " + error.getMessage());
            }
        }) ;

    }

    // Task: Inflates and shows a dialog to register a new driver by adding a User record first and then creating the matching Driver record linked by ID.
    // Input: None
    // Output: None
    private void showAddDriverDialog() {
        View dialogView = LayoutInflater.from(AdminActivity.this)
                .inflate(R.layout.dialog_add_driver, null);

        EditText etName  = dialogView.findViewById(R.id.etAddName);
        EditText etPass  = dialogView.findViewById(R.id.etAddPassword);
        EditText etEmail = dialogView.findViewById(R.id.etAddEmail);
        EditText etPhone = dialogView.findViewById(R.id.etAddPhone);
        EditText etImage = dialogView.findViewById(R.id.etAddImage);

        new AlertDialog.Builder(AdminActivity.this)
                .setTitle("Add New Driver")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name  = etName.getText().toString().trim();
                    String pass  = etPass.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String image = etImage.getText().toString().trim();

                    if (name.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(AdminActivity.this, "Name and password required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    User newUser = new User(null, name, pass, email, phone,
                            image.isEmpty() ? DEFAULT_IMAGE_URL : image, "driver");

                    userRepo.addUser(newUser, new BaseRepo.RepoCallback<User>() {
                        @Override
                        public void onSuccess(User addedUser) {
                            Driver newDriver = new Driver(addedUser.getId(), "available");
                            driverRepo.addUser(newDriver, new BaseRepo.RepoCallback<Driver>() {
                                @Override
                                public void onSuccess(Driver result) {
                                    runOnUiThread(() -> {
                                        DriverWithUser dwu = new DriverWithUser();
                                        dwu.setId(result.getId());
                                        dwu.setStatus(result.getStatus());
                                        dwu.setUsers(addedUser);
                                        driversList.add(dwu);
                                        adapter.notifyItemInserted(driversList.size() - 1);
                                        Toast.makeText(AdminActivity.this, "Driver added!", Toast.LENGTH_SHORT).show();
                                    });
                                }
                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() ->
                                            Toast.makeText(AdminActivity.this, "Failed to add driver", Toast.LENGTH_SHORT).show());
                                }
                            });
                        }
                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(AdminActivity.this, "Failed to add user", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    // Task: Displays a pre-filled dialog allowing the admin to update an existing driver's email, phone, and image URL properties.
    // Input: driver (DriverWithUser), position (int)
    // Output: None
    private void showUpdateDriverDialog(DriverWithUser driver, int position) {
        View dialogView = LayoutInflater.from(AdminActivity.this)
                .inflate(R.layout.dialog_update_driver, null);

        EditText etEmail = dialogView.findViewById(R.id.etEmailDriver);
        EditText etPhone = dialogView.findViewById(R.id.etPhoneDriver);
        EditText etImage = dialogView.findViewById(R.id.etImageDriver);

        // Pre-fill with current values
        if (driver.getUsers() != null) {
            etEmail.setText(driver.getUsers().getEmail());
            etPhone.setText(driver.getUsers().getPhone());
            etImage.setText(driver.getUsers().getImageUrl());
        }

        new AlertDialog.Builder(AdminActivity.this)
                .setTitle("Update Driver")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (driver.getUsers() == null) return;

                    driver.getUsers().setEmail(etEmail.getText().toString().trim());
                    driver.getUsers().setPhone(etPhone.getText().toString().trim());
                    String image = etImage.getText().toString().trim();
                    driver.getUsers().setImageUrl(image.isEmpty() ? DEFAULT_IMAGE_URL : image);

                    UserRepo repo = new UserRepo();
                    repo.updateUser(driver.getUsers(), new BaseRepo.RepoCallback<User>() {
                        @Override
                        public void onSuccess(User result) {
                            runOnUiThread(() -> {
                                adapter.notifyItemChanged(position);
                                Toast.makeText(AdminActivity.this, "Driver updated!", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override
                        public void onError(Exception error) {
                            runOnUiThread(() ->
                                    Toast.makeText(AdminActivity.this, "Update failed", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyDataSetChanged())
                .show();
    }

    // Task: Sends an HTTP PATCH network request to update specific data attributes of a driver inside the remote Supabase database.
    // Input: driver (Driver)
    // Output: None
    private void updateDriverInSupabase(Driver driver) {
        String url = DriverRepo.SUPABASE_URL + "/rest/v1/drivers?id=eq." + driver.getId();

        // Prepare JSON
        Gson gson = new Gson();
        String json = gson.toJson(driver);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", DriverRepo.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + DriverRepo.SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Repository", "Error updating ride", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("Repository", "Ride updated successfully");
                } else {
                    Log.e("Repository", "Update failed: " + response.code() + " " + response.message());
                }
            }
        });
    }

    // Task: Temporarily removes a driver from the local view list with a Snackbar option to UNDO; if dismissed, finalizes the irreversible deletion from database tables.
    // Input: driver (DriverWithUser), position (int)
    // Output: None
    private void deleteDriver(DriverWithUser driver, int position) {
        DriverWithUser deleted = driversList.remove(position);
        adapter.notifyItemRemoved(position);

        Snackbar.make(recyclerDrivers, "Driver deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> {
                    driversList.add(position, deleted);
                    adapter.notifyItemInserted(position);
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            // Delete from drivers table
                            deleteDriverFromSupabase(deleted.toDriver());
                            // Also delete from users table
                            if (deleted.getUsers() != null) {
                                userRepo.deleteUser(deleted.getUsers().getId(),
                                        new BaseRepo.RepoCallback<Boolean>() {
                                            @Override public void onSuccess(Boolean result) {}
                                            @Override public void onError(Exception error) {}
                                        });
                            }
                        }
                    }
                })
                .show();
    }

    // Task: Executes an asynchronous HTTP DELETE backend call to wipe a driver record completely out of the Supabase platform.
    // Input: driver (Driver)
    // Output: None
    private void deleteDriverFromSupabase(Driver driver) {
        String url = DriverRepo.SUPABASE_URL + "/rest/v1/drivers?id=eq." + driver.getId();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", DriverRepo.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + DriverRepo.SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Repository", "Error deleting driver", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("Repository", "driver deleted from Supabase");
                } else {
                    Log.e("Repository", "Failed to delete driver: " + response.code() + " " + response.message());
                }
            }
        });
    }
}