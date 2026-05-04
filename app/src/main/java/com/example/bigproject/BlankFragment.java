package com.example.bigproject;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class BlankFragment extends Fragment {
    private RecyclerView recyclerRides;
    private RideAdapter adapter;
    private ArrayList<DriverWithUser> driversList; // Matches AdminActivity
    private FloatingActionButton fabAdd;
    private DriverRepo driverRepo;

    public BlankFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerRides = view.findViewById(R.id.recyclerRides);
        fabAdd = view.findViewById(R.id.fabAction);
        driverRepo = new DriverRepo();
        driversList = new ArrayList<>();

        // Fix: Adapter now handles DriverWithUser
        adapter = new RideAdapter(driversList, new RideAdapter.OnItemActionListener() {
            @Override
            public void onEdit(DriverWithUser driver, int position) {
                showUpdateRideDialog(driver, position);
            }

            @Override
            public void onDelete(DriverWithUser driver, int position) {
                deleteRide(driver, position);
            }

            @Override
            public void onClick(DriverWithUser driver, int position) {
                Toast.makeText(getContext(), "Clicked: " + driver.getId(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerRides.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerRides.setAdapter(adapter);

        getUsers();

        fabAdd.setOnClickListener(v -> showAddRideDialog());

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                DriverWithUser driver = driversList.get(position);
                if (direction == ItemTouchHelper.RIGHT) showUpdateRideDialog(driver, position);
                else if (direction == ItemTouchHelper.LEFT) deleteRide(driver, position);
            }
        }).attachToRecyclerView(recyclerRides);
    }

    public void getUsers() {
        driverRepo.getDriversWithUsers(new BaseRepo.RepoCallback<List<DriverWithUser>>() {
            @Override
            public void onSuccess(List<DriverWithUser> result) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        driversList.clear();
                        driversList.addAll(result);
                        adapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e("BlankFragment", "Error: " + error.getMessage());
            }
        });
    }

    private void showAddRideDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_ride, null);
        EditText etLocation = dialogView.findViewById(R.id.etCurrentLocation);
        EditText etStatus = dialogView.findViewById(R.id.etStatus);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Ride")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String loc = etLocation.getText().toString().trim();
                    String stat = etStatus.getText().toString().trim();
                    if (loc.isEmpty() || stat.isEmpty()) return;

                    Driver newRide = new Driver(null, loc, stat);
                    driverRepo.addUser(newRide, new DriverRepo.RepoCallback<Driver>() {
                        @Override
                        public void onSuccess(Driver result) {
                            requireActivity().runOnUiThread(() -> {
                                // Manual conversion to DriverWithUser
                                DriverWithUser dwu = new DriverWithUser();
                                dwu.setId(result.getId());
                                dwu.setCurrentLocation(result.getCurrentLocation());
                                dwu.setStatus(result.getStatus());
                                dwu.setUsers(null);

                                driversList.add(dwu);
                                adapter.notifyItemInserted(driversList.size() - 1);
                            });
                        }
                        @Override
                        public void onError(Exception e) { /* handle error */ }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUpdateRideDialog(DriverWithUser driver, int position) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_ride, null);
        EditText etLocation = dialogView.findViewById(R.id.etCurrentLocation);
        EditText etStatus = dialogView.findViewById(R.id.etStatus);

        etLocation.setText(driver.getCurrentLocation());
        etStatus.setText(driver.getStatus());

        new AlertDialog.Builder(getContext())
                .setTitle("Update Ride")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    driver.setCurrentLocation(etLocation.getText().toString());
                    driver.setStatus(etStatus.getText().toString());

                    // Convert to Driver for the Repo call
                    Driver d = new Driver(driver.getId(), driver.getCurrentLocation(), driver.getStatus());
                    updateRideInSupabase(d, position);
                })
                .setNegativeButton("Cancel", (d, w) -> adapter.notifyItemChanged(position))
                .show();
    }

    private void updateRideInSupabase(Driver d, int pos) {
        driverRepo.updateDriver(d, new DriverRepo.RepoCallback<Driver>() {
            @Override
            public void onSuccess(Driver result) {
                requireActivity().runOnUiThread(() -> adapter.notifyItemChanged(pos));
            }
            @Override
            public void onError(Exception e) {
                requireActivity().runOnUiThread(() -> adapter.notifyItemChanged(pos));
            }
        });
    }

    private void deleteRide(DriverWithUser driver, int position) {
        DriverWithUser deletedItem = driversList.remove(position);
        adapter.notifyItemRemoved(position);

        Snackbar.make(recyclerRides, "Ride deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO", v -> {
                    driversList.add(position, deletedItem);
                    adapter.notifyItemInserted(position);
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            Driver d = new Driver(deletedItem.getId(), deletedItem.getCurrentLocation(), deletedItem.getStatus());
                            deleteRideFromSupabase(d);
                        }
                    }
                }).show();
    }

    private void deleteRideFromSupabase(Driver d) {
        driverRepo.deleteDriver(d, new DriverRepo.RepoCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) { Log.d("Delete", "Success"); }
            @Override
            public void onError(Exception e) { Log.e("Delete", e.getMessage()); }
        });
    }
}