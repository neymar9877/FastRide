package com.example.bigproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;


public class LoginActivity extends AppCompatActivity {
    private EditText etUserName, etPassword;
    private Button btnLogin;
    private Button btnSignUp;
    private CheckBox cbRememberme;
    private SharedPreferences sp;

    String deafultImageUrl = "https://img.freepik.com/premium-vector/default-male-user-profile-icon-vector-illustration_276184-168.jpg";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        etUserName = findViewById(R.id.etUserName);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        cbRememberme = findViewById(R.id.cbRememberme);

        sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
        checkIfFIrstRun();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = etUserName.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (userName.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                handleLogin(userName, password);
            }
        });


        btnSignUp.setOnClickListener(v -> showSignUpDialog());
    }

    // Task: Inflates and displays a custom dialog for user registration, handles validation, and updates both users and drivers databases.
    // Input: None
    // Output: None
    private void showSignUpDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signup_details, null);

        EditText etName  = dialogView.findViewById(R.id.etSignUpName);
        EditText etPass  = dialogView.findViewById(R.id.etSignUpPassword);
        EditText etEmail = dialogView.findViewById(R.id.etSignUpEmail);
        EditText etPhone = dialogView.findViewById(R.id.etSignUpPhone);
        EditText etImage = dialogView.findViewById(R.id.etSignUpImage);
        RadioGroup rgRole = dialogView.findViewById(R.id.rgRole);

        new AlertDialog.Builder(this)
                .setTitle("Create Account")
                .setView(dialogView)
                .setPositiveButton("Sign Up", (dialog, which) -> {
                    String name  = etName.getText().toString().trim();
                    String pass  = etPass.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String image = etImage.getText().toString().trim();

                    if (name.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, "Name and password are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedId = rgRole.getCheckedRadioButtonId();
                    String role = (selectedId == R.id.rbDriver) ? "driver" : "passenger";

                    String finalImage = image.isEmpty()
                            ? "https://img.freepik.com/premium-vector/default-male-user-profile-icon-vector-illustration_276184-168.jpg"
                            : image;

                    User newUser = new User(null, name, pass, email, phone, finalImage, role);

                    UserRepo repo = new UserRepo();
                    repo.addUser(newUser, new BaseRepo.RepoCallback<User>() {
                        @Override
                        public void onSuccess(User result) {
                            runOnUiThread(() -> {
                                sp.edit().putString("userId", result.getId()).apply();

                                // If role is driver, also add to drivers table
                                if (role.equals("driver")) {
                                    DriverRepo driverRepo = new DriverRepo();
                                    Driver newDriver = new Driver(result.getId(), "available");
                                    driverRepo.addUser(newDriver, new BaseRepo.RepoCallback<Driver>() {
                                        @Override
                                        public void onSuccess(Driver d) {
                                            Log.d("SignUp", "Driver added to drivers table");
                                        }
                                        @Override
                                        public void onError(Exception e) {
                                            Log.e("SignUp", "Failed to add to drivers table: " + e.getMessage());
                                        }
                                    });
                                }

                                Toast.makeText(LoginActivity.this,
                                        "Account created! Please login.", Toast.LENGTH_SHORT).show();
                                etUserName.setText(name);
                            });
                        }
                        @Override
                        public void onError(Exception error) {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this,
                                            "Sign up failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Task: Checks SharedPreferences to see if "Remember Me" was enabled, and automatically logs in if credentials exist.
    // Input: None
    // Output: None
    private void checkIfFIrstRun(){
        boolean remember = sp.getBoolean("remember", false);
        if (!remember) return;


        String savedName = sp.getString("userName", "");
        String savedPassword = sp.getString("password", "");

        if (!savedName.isEmpty() || !savedPassword.isEmpty()) { // check if fields have text
            handleLogin(savedName, savedPassword);
        }
    }


    // Task: Fetches all users from database to validate credentials, manages session memory, and handles "Remember Me" logic.
    // Input: userName (String), Password (String)
    // Output: None
    public void handleLogin(String userName, String Password){
        UserRepo repo = new UserRepo();
        UserRepo.getAllUsers(new UserRepo.RepoCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                boolean userFound = false; // flag
                for (User user: result){
                    if (userName.equals(user.getUserName()) && Password.equals(user.getPassword())){
                        // Always save userId for current session
                        sp.edit().putString("userId", user.getId()).apply();

                        if (cbRememberme.isChecked()) {
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putBoolean("remember", true);
                            editor.putString("userName", userName);
                            editor.putString("password", Password);
                            editor.apply();
                        }
                        // switch to entry page (according to the role)
                        runOnUiThread(() -> { // back to main thread
                            switchScreen(user.getRole());
                        });
                        userFound = true;
                    }
                    Log.d("user isss: ", user.getPassword() + user.getUserName());
                }
                if(!userFound){
                    runOnUiThread(() -> { // back to main thread
                        Toast.makeText(LoginActivity.this, "User not found!!", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e("Error fetching users", error.getMessage());
            }
        });
    }

    // Task: Redirects the user to the corresponding activity dashboard (Admin, Passenger, or Driver) based on their role.
    // Input: role (String)
    // Output: None
    public void switchScreen(String role) {
        Intent intent;

        switch (role) {
            case "admin":
                intent = new Intent(LoginActivity.this, AdminActivity.class);
                break;

            case "passenger":
                intent = new Intent(LoginActivity.this, MainActivity.class);
                break;

            case "driver":
                intent = new Intent(LoginActivity.this, DriverActivity.class);
                break;

            default:
                Toast.makeText(
                        LoginActivity.this,
                        "Unknown user role",
                        Toast.LENGTH_SHORT
                ).show();
                return;
        }

        startActivity(intent);
        // finish(); // optional: prevents going back to login
    }


}