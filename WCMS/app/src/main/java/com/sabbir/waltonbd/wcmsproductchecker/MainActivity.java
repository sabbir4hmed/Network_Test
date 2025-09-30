package com.sabbir.waltonbd.wcmsproductchecker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sabbir.waltonbd.wcmsproductchecker.Adapter.ProductAdapter;
import com.sabbir.waltonbd.wcmsproductchecker.Api.ApiService;
import com.sabbir.waltonbd.wcmsproductchecker.Api.RetrofitClient;
import com.sabbir.waltonbd.wcmsproductchecker.Models.ProductResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int BARCODE_SCANNER_REQUEST = 200;

    // UI Components
    private TextView tvUserName, tvEmployeeId, tvScannedImei;
    private ImageButton btnLogout;
    private MaterialButton btnScanProduct, btnRescan, btnSearch;
    private MaterialCardView cardScannedImei;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextInputLayout tilManualImei;
    private TextInputEditText etManualImei;

    // Data
    private SharedPreferences sharedPreferences;
    private ApiService apiService;
    private ProductAdapter productAdapter;
    private String scannedCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initData();
        loadUserInfo();
        setupRecyclerView();
        setClickListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvScannedImei = findViewById(R.id.tvScannedImei);
        btnLogout = findViewById(R.id.btnLogout);
        btnScanProduct = findViewById(R.id.btnScanProduct);
        btnRescan = findViewById(R.id.btnRescan);
        btnSearch = findViewById(R.id.btnSearch);
        cardScannedImei = findViewById(R.id.cardScannedImei);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        tilManualImei = findViewById(R.id.tilManualImei);
        etManualImei = findViewById(R.id.etManualImei);
    }

    private void initData() {
        sharedPreferences = getSharedPreferences("WCMSPrefs", MODE_PRIVATE);
        apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    private void loadUserInfo() {
        String userName = sharedPreferences.getString("userName", "User");
        String employeeId = sharedPreferences.getString("employeeId", "00000");

        tvUserName.setText("Welcome, " + userName);
        tvEmployeeId.setText("ID: " + employeeId);
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(productAdapter);
    }

    private void setClickListeners() {
        // Scan Product Button
        btnScanProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermissionAndScan();
            }
        });

        // Rescan Button
        btnRescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermissionAndScan();
            }
        });

        // Search Button
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performManualSearch();
            }
        });

        // Logout Button
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });

        // Handle keyboard search action
        etManualImei.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performManualSearch();
                    return true;
                }
                return false;
            }
        });
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            // Permission already granted, start scanner
            startBarcodeScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                startBarcodeScanner();
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission is required to scan barcodes",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Start the barcode scanner activity
     */
    private void startBarcodeScanner() {
        Intent intent = new Intent(this, BarcodeScannerActivity.class);
        startActivityForResult(intent, BARCODE_SCANNER_REQUEST);
    }

    /**
     * Perform manual search from text input
     */
    private void performManualSearch() {
        // Get input value
        String manualImei = etManualImei.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(manualImei)) {
            tilManualImei.setError("Please enter IMEI or Code");
            etManualImei.requestFocus();
            return;
        }

        // Clear error
        tilManualImei.setError(null);

        // Clean the input (remove spaces, special characters)
        manualImei = manualImei.replaceAll("\\s+", "");

        Log.d(TAG, "Manual Search IMEI: " + manualImei);

        // Process the manual input
        handleScannedCode(manualImei);

        // Hide keyboard
        etManualImei.clearFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BARCODE_SCANNER_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                String result = data.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE_RESULT);
                if (result != null && !result.isEmpty()) {
                    scannedCode = result;
                    Log.d(TAG, "Scanned: " + scannedCode);
                    handleScannedCode(scannedCode);
                } else {
                    Toast.makeText(this, "Invalid barcode data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleScannedCode(String code) {
        // Clean the code (remove spaces, newlines, etc.)
        code = code.trim().replaceAll("\\s+", "");

        Log.d(TAG, "==================== HANDLE SCANNED CODE ====================");
        Log.d(TAG, "Original Code: [" + code + "]");
        Log.d(TAG, "Code Length: " + code.length());

        // Show scanned code
        tvScannedImei.setText(code);
        cardScannedImei.setVisibility(View.VISIBLE);
        btnRescan.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        // Clear manual input
        etManualImei.setText("");

        // Fetch product details
        fetchProductDetails(code);
    }

    private void fetchProductDetails(String codeOrImei) {
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        Log.d(TAG, "==================== API CALL START ====================");
        Log.d(TAG, "Fetching product details for: " + codeOrImei);
        Log.d(TAG, "IMEI Length: " + codeOrImei.length());

        // Make API call
        Call<ProductResponse> call = apiService.getProductByImei(codeOrImei);

        call.enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                progressBar.setVisibility(View.GONE);

                Log.d(TAG, "==================== API RESPONSE ====================");
                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Message: " + response.message());
                Log.d(TAG, "Request URL: " + call.request().url());

                // Log response body even if null
                if (response.body() != null) {
                    Log.d(TAG, "Response Body: " + response.body().toString());
                    Log.d(TAG, "Model: " + response.body().getModel());
                    Log.d(TAG, "Mobile Code: " + response.body().getMobileCode());
                    Log.d(TAG, "IMEI1: " + response.body().getImei1());
                    Log.d(TAG, "IMEI2: " + response.body().getImei2());
                } else {
                    Log.e(TAG, "Response Body is NULL");

                    // Try to get error body
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error Body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }

                if (response.isSuccessful() && response.body() != null) {
                    ProductResponse product = response.body();

                    // Show product in RecyclerView
                    productAdapter.setProduct(product);
                    recyclerView.setVisibility(View.VISIBLE);

                    Toast.makeText(MainActivity.this,
                            "Product found: " + product.getModel(),
                            Toast.LENGTH_SHORT).show();

                } else {
                    // Product not found
                    String errorMessage = handleHttpError(response.code());

                    Log.e(TAG, "Error: " + errorMessage);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                    showErrorState(errorMessage);
                }

                Log.d(TAG, "==================== API CALL END ====================");
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);

                Log.e(TAG, "==================== API FAILURE ====================");
                Log.e(TAG, "Request URL: " + call.request().url());
                Log.e(TAG, "Failure Message: " + t.getMessage());
                Log.e(TAG, "Failure Class: " + t.getClass().getName());
                t.printStackTrace();

                String errorMessage = "Failed to fetch product details: " + t.getMessage();
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                showErrorState("Connection failed. Please try again.");
                Log.e(TAG, "==================== API FAILURE END ====================");
            }
        });
    }

    /**
     * Handle HTTP error codes
     */
    private String handleHttpError(int code) {
        switch (code) {
            case 400:
                return "Invalid code/IMEI format";
            case 404:
                return "No product found with this code/IMEI";
            case 500:
                return "Server error. Please try again later";
            default:
                return "Product not found (Error: " + code + ")";
        }
    }

    private void showErrorState(String message) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .show();
    }

    private void performLogout() {
        // Clear SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Navigate to Login Activity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        // Show exit confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                .setNegativeButton("No", null)
                .show();
    }
}
