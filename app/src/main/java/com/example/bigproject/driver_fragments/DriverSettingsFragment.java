package com.example.bigproject.driver_fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bigproject.AdminActivity;
import com.example.bigproject.BaseRepo;
import com.example.bigproject.Driver;
import com.example.bigproject.DriverRepo;
import com.example.bigproject.LoginActivity;
import com.example.bigproject.R;
import com.example.bigproject.User;
import com.example.bigproject.UserRepo;

public class DriverSettingsFragment extends Fragment {
    View logoutBtn;
    View  updateProfileBtn;


    public DriverSettingsFragment() {
        // Required empty public constructor
    }
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_driver_settings, container, false);
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void onViewCreated(View view,Bundle savedInstanceState ) {
        super.onViewCreated(view, savedInstanceState);

        logoutBtn = view.findViewById(R.id.logout_btn);
        updateProfileBtn = view.findViewById(R.id.update_profile_btn);



        logoutBtn.setOnClickListener(v -> showLogoutDialog());

        updateProfileBtn.setOnClickListener(v -> openUpdateProfile());
    }

    // ================= UPDATE PROFILE =================


    private void openUpdateProfile() {
        SharedPreferences sp =
                requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);

        String userName = sp.getString("userName", null);

        if (userName == null) {
            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        UserRepo userRepo = new UserRepo();
        userRepo.CheckUserExist(userName, new BaseRepo.RepoCallback<User>() {
            @Override
            public void onSuccess(User result) {
                requireActivity().runOnUiThread(() ->
                        showUpdateDialog(result));
            }

            @Override
            public void onError(Exception error) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showUpdateDialog(User user) {
        String default_image = AdminActivity.DEFAULT_IMAGE_URL;
        // Inflate only the dialog layout
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_update_driver, null);

        EditText etEmail  = dialogView.findViewById(R.id.etEmailDriver);
        EditText etPhone  = dialogView.findViewById(R.id.etPhoneDriver);
        EditText etImage  = dialogView.findViewById(R.id.etImageDriver);

        etEmail.setText(user.getEmail());
        etPhone.setText(user.getPhone());
        etImage.setText(user.getImageUrl());

        new AlertDialog.Builder(requireContext())
                .setTitle("Update Profile")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    user.setEmail(etEmail.getText().toString().trim());
                    user.setPhone(etPhone.getText().toString().trim());
                    String image = etImage.getText().toString().trim();
                    user.setImageUrl(image.isEmpty()
                            ? default_image
                            : image);
                    UserRepo repo = new UserRepo();
                    repo.updateUser(user, new BaseRepo.RepoCallback<User>() {
                        @Override
                        public void onSuccess(User result) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(
                                            requireContext(),
                                            "Profile updated",
                                            Toast.LENGTH_SHORT
                                    ).show());
                        }

                        @Override
                        public void onError(Exception error) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(
                                            requireContext(),
                                            "Update failed",
                                            Toast.LENGTH_SHORT
                                    ).show());
                        }
                    });

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==========================================
    // ================= LOGOUT =================
    // ==========================================

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        Context context = requireContext();

        // Clear SharedPreferences
        SharedPreferences sp =
                context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();

        // Go back to LoginActivity and clear back stack
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}
