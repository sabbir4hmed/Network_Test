package com.sabbir.waltonbd.wcmsproductchecker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sabbir.waltonbd.wcmsproductchecker.Api.ApiService;
import com.sabbir.waltonbd.wcmsproductchecker.Api.RetrofitClient;
import com.sabbir.waltonbd.wcmsproductchecker.Models.LoginRequest;
import com.sabbir.waltonbd.wcmsproductchecker.Models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputLayout tilEmployeeId, tilPassword;
    private TextInputEditText etEmployeeId, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        initViews();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("WCMSPrefs", MODE_PRIVATE);

        // Initialize API Service
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Check if already logged in
        checkLoginStatus();

        // Set click listeners
        setClickListeners();
    }

    private void initViews() {
        tilEmployeeId = findViewById(R.id.tilEmployeeId);
        tilPassword = findViewById(R.id.tilPassword);
        etEmployeeId = findViewById(R.id.etEmployeeId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void checkLoginStatus() {
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            // Navigate to main activity
            navigateToMainActivity();
        }
    }

    private void setClickListeners() {
        // Login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        // Forgot password click
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this,
                        "Please contact with IT Team",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void attemptLogin() {
        // Reset errors
        tilEmployeeId.setError(null);
        tilPassword.setError(null);

        // Get input values
        String employeeId = etEmployeeId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        boolean isValid = true;

        if (TextUtils.isEmpty(employeeId)) {
            tilEmployeeId.setError("Employee ID is required");
            etEmployeeId.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            if (isValid) {
                etPassword.requestFocus();
            }
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Check internet connection
        if (!isInternetAvailable()) {
            Toast.makeText(this, "Please connect your internet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Perform login
        performLogin(employeeId, password);
    }

    private void performLogin(String employeeId, String password) {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        // Create login request
        LoginRequest loginRequest = new LoginRequest(employeeId, password);

        Log.d(TAG, "Attempting login for user: " + employeeId);

        // Make API call
        Call<LoginResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                // Hide progress bar
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");

                Log.d(TAG, "Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    Log.d(TAG, "Response ID: " + loginResponse.getId());
                    Log.d(TAG, "Response Name: " + loginResponse.getName());

                    if (loginResponse.isSuccess()) {
                        // Login successful
                        handleSuccessfulLogin(loginResponse, employeeId);
                    } else {
                        // Login failed
                        Toast.makeText(LoginActivity.this,
                                "Login failed. Please check your credentials.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // HTTP error
                    String errorMessage = "Login failed. ";
                    if (response.code() == 401) {
                        errorMessage += "Invalid credentials.";
                    } else if (response.code() == 404) {
                        errorMessage += "Service not found.";
                    } else if (response.code() == 500) {
                        errorMessage += "Server error. Please try again later.";
                    } else {
                        errorMessage += "Error code: " + response.code();
                    }

                    Log.e(TAG, "Login Error: " + errorMessage);
                    //Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // Hide progress bar
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");

                // Network error
                String errorMessage = "Connection failed: ";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("CLEARTEXT")) {
                        errorMessage = "Network security error. Please check your connection.";
                    } else {
                        errorMessage += t.getMessage();
                    }
                } else {
                    errorMessage += "Unknown error";
                }

                Log.e(TAG, "Login Failure: " + errorMessage, t);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void handleSuccessfulLogin(LoginResponse loginResponse, String employeeId) {
        // Save login data to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("employeeId", employeeId);

        // Save user data from response
        if (loginResponse.getId() > 0) {
            editor.putString("userId", String.valueOf(loginResponse.getId()));
        }

        if (loginResponse.getUserName() != null && !loginResponse.getUserName().isEmpty()) {
            editor.putString("username", loginResponse.getUserName());
        }

        if (loginResponse.getName() != null && !loginResponse.getName().isEmpty()) {
            editor.putString("userName", loginResponse.getName());
        }

        if (loginResponse.getRole() != null && !loginResponse.getRole().isEmpty()) {
            editor.putString("userRole", loginResponse.getRole());
        }

        // Apply all changes
        editor.apply();

        Log.d(TAG, "Login successful for user: " + employeeId);
        Log.d(TAG, "User ID: " + loginResponse.getId());
        Log.d(TAG, "User Name: " + loginResponse.getName());
        Log.d(TAG, "User Role: " + loginResponse.getRole());

        // Show success message
        String welcomeMessage = "Login Successful! Welcome " + loginResponse.getName();
        Toast.makeText(LoginActivity.this, welcomeMessage, Toast.LENGTH_SHORT).show();

        // Navigate to main activity
        navigateToMainActivity();
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Exit app on back press from login screen
        finishAffinity();
    }
}
