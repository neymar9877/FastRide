package com.example.bigproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
        cbRememberme = findViewById(R.id.checkBoxId);

        sp = getSharedPreferences("myPrefs", MODE_PRIVATE);
        checkIfFIrstRun();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String UserName = etUserName.getText().toString().trim();
                String Password = etPassword.getText().toString().trim();

                if (UserName.isEmpty() || Password.isEmpty()) { // check if fields have text
                    Toast.makeText(LoginActivity.this, "Fill both fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                handleLogin(UserName, Password);
            }
        });


        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String UserName = etUserName.getText().toString().trim();
                String Password = etPassword.getText().toString().trim();

                if (UserName.isEmpty() || Password.isEmpty()) { // check if fields have text
                    Toast.makeText(LoginActivity.this, "Fill both fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                checkIfUserExistsAndContinue(UserName, Password);
            }
        });
    }

    private void checkIfFIrstRun(){
        boolean remember = sp.getBoolean("remember", false);
        if (!remember) return;


        String savedName = sp.getString("userName", "");
        String savedPassword = sp.getString("password", "");

        if (!savedName.isEmpty() || !savedPassword.isEmpty()) { // check if fields have text
            handleLogin(savedName, savedPassword);
        }
    }

    private void checkIfUserExistsAndContinue(String userName, String password) {
        UserRepo.getAllUsers(new UserRepo.RepoCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                for (User user : result) {
                    if (userName.equals(user.getUserName())) {
                        runOnUiThread(() ->
                                Toast.makeText(
                                        LoginActivity.this,
                                        "Username already exists",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
                        return;
                    }
                }
                // Username is free → continue to details dialog
                runOnUiThread(() -> showSignUpDetailsDialog(userName, password));
            }


            @Override
            public void onError(Exception error) {
                runOnUiThread(() ->
                        Toast.makeText(
                                LoginActivity.this,
                                "Error checking users",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }
        });
    }

    private void showSignUpDetailsDialog(String userName, String password) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Complete Sign Up");

        View view = getLayoutInflater().inflate(R.layout.dialog_signup_details, null);
        builder.setView(view);

        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etImage = view.findViewById(R.id.etImage);
        Spinner spinnerRole = view.findViewById(R.id.spinnerRole);

        // get the role from the spinner
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        this,
                        R.array.roles_array,
                        android.R.layout.simple_spinner_item
                );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);


        builder.setPositiveButton("Create Account", null);
        builder.setNegativeButton("Cancel", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String image = etImage.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();

            if (email.isEmpty() || phone.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }
            if(image.isEmpty()){
                image = deafultImageUrl;
            }

            User newUser = new User(null, userName, password, email, phone, image, role);
            createUser(newUser, dialog);
        });

    }

    private void createUser(User user, AlertDialog dialog) {
        UserRepo repo = new UserRepo();

        repo.addUser(user, new UserRepo.RepoCallback<User>() {
            @Override
            public void onSuccess(User createdUser) {
                runOnUiThread(() -> {
                    Toast.makeText(
                            LoginActivity.this,
                            "User created successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                    dialog.dismiss();

                    //  auto login after sign up
                    switchScreen(createdUser.getRole());
                    finish();
                });
            }

            @Override
            public void onError(Exception error) {runOnUiThread(() ->
                    Toast.makeText(
                            LoginActivity.this,
                            "Failed to create user",
                            Toast.LENGTH_SHORT
                    ).show()
            );

            }
        });
    }

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