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

    private void showAddDriverDialog() {
        // Use your new XML for User details (Name, Image, Phone)
        View dialogView = LayoutInflater.from(AdminActivity.this).inflate(R.layout.dialog_user, null);
        EditText etName = dialogView.findViewById(R.id.etUserName);
        EditText etPhone = dialogView.findViewById(R.id.etUserPhone);
        EditText etImage = dialogView.findViewById(R.id.etUserImage);

        new AlertDialog.Builder(AdminActivity.this)
                .setTitle("Add New Driver")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();

                    // 1. Create the User first
                    User newUser = new User(null, name, "", "", etPhone.getText().toString(), etImage.getText().toString(), "driver");


                    userRepo.addUser(newUser, new UserRepo.RepoCallback<User>() {
                        @Override
                        public void onSuccess(User addedUser) {
                            // 2. Now create the Driver entry using the same ID
                            Driver newDriver = new Driver(addedUser.getId(), "Pending", "Active");
                            driverRepo.addUser(newDriver, new DriverRepo.RepoCallback<Driver>() {
                                @Override
                                public void onSuccess(Driver result) {
                                    runOnUiThread(() -> {
                                        DriverWithUser dwu = new DriverWithUser();
                                        dwu.setId(result.getId());
                                        dwu.setCurrentLocation(result.getCurrentLocation());
                                        dwu.setStatus(result.getStatus());
                                        dwu.setUsers(addedUser);

                                        driversList.add(dwu);
                                        adapter.notifyItemInserted(driversList.size() - 1);
                                    });
                                }
                                @Override
                                public void onError(Exception e) { /* handle */ }
                            });
                        }
                        @Override
                        public void onError(Exception e) { /* handle */ }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showUpdateDriverDialog(DriverWithUser driver, int position) {
        View dialogView = LayoutInflater.from(AdminActivity.this).inflate(R.layout.dialog_add_ride, null);
        EditText etCurrentLocation = dialogView.findViewById(R.id.etCurrentLocation);
        EditText etStatus = dialogView.findViewById(R.id.etStatus);


        etCurrentLocation.setText(driver.getCurrentLocation());
        etStatus.setText(driver.getStatus());

        new AlertDialog.Builder(AdminActivity.this)
                .setTitle("Update Ride")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // 1. Update the local data for the UI
                    driver.setCurrentLocation(etCurrentLocation.getText().toString());
                    driver.setStatus(etStatus.getText().toString());
                    adapter.notifyItemChanged(position);

                    // 2. Convert DriverWithUser to a plain Driver object
                    Driver driverToUpdate = new Driver(
                            driver.getId(),
                            driver.getCurrentLocation(),
                            driver.getStatus()
                    );

                    // 3. Pass the CORRECT type (Driver) to the method
                    updateDriverInSupabase(driverToUpdate);
                })

                .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyDataSetChanged())
                .show();
    }

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

    private void deleteDriver(DriverWithUser driver, int position) {
        // 1️⃣ Temporarily remove from list
        DriverWithUser deletedrive = driversList.remove(position);
        adapter.notifyItemRemoved(position);

        // 2️⃣ Show Undo Snackbar
        Snackbar.make(recyclerDrivers, "driver deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> {
                    // 3️⃣ User tapped UNDO → restore
                    driversList.add(position, deletedrive);
                    adapter.notifyItemInserted(position);
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            // 4️⃣ Snackbar timed out → actually delete from Supabase
                            Driver driverToDelete = deletedrive.toDriver();
                            deleteDriverFromSupabase(driverToDelete);
                        }
                    }
                })
                .show();
    }

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