package com.sabbir.waltonbd.wcmsproductchecker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.budiyev.android.codescanner.AutoFocusMode;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.budiyev.android.codescanner.ScanMode;
import com.google.zxing.Result;

public class BarcodeScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    public static final String EXTRA_BARCODE_RESULT = "barcode_result";

    private CodeScanner codeScanner;
    private CodeScannerView scannerView;
    private ImageButton btnFlashlight;
    private ImageButton btnClose;
    private TextView tvInstruction;
    private boolean isFlashlightOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        initViews();

        if (checkCameraPermission()) {
            setupScanner();
        } else {
            requestCameraPermission();
        }
    }

    private void initViews() {
        scannerView = findViewById(R.id.scanner_view);
        btnFlashlight = findViewById(R.id.btnFlashlight);
        btnClose = findViewById(R.id.btnClose);
        tvInstruction = findViewById(R.id.tvInstruction);

        // Set click listeners
        btnFlashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan barcodes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setupScanner() {
        codeScanner = new CodeScanner(this, scannerView);

        // Configure scanner parameters
        codeScanner.setCamera(CodeScanner.CAMERA_BACK);  // Use back camera
        codeScanner.setFormats(CodeScanner.ALL_FORMATS);  // Support all barcode formats

        // Set to CONTINUOUS mode for fixed focus (no auto-focus)
        codeScanner.setAutoFocusMode(AutoFocusMode.CONTINUOUS);

        codeScanner.setScanMode(ScanMode.SINGLE);  // Single scan mode
        codeScanner.setAutoFocusEnabled(true);  // Disable auto focus for fixed focus
        codeScanner.setFlashEnabled(false);  // Flash off by default
        codeScanner.setTouchFocusEnabled(false);  // Disable touch to focus

        // Set decode callback
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String scannedCode = result.getText();
                        returnResult(scannedCode);
                    }
                });
            }
        });

        // Set error callback
        codeScanner.setErrorCallback(error -> {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BarcodeScannerActivity.this,
                            "Camera error: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Start preview
        codeScanner.startPreview();
    }

    private void toggleFlashlight() {
        if (codeScanner != null) {
            try {
                isFlashlightOn = !isFlashlightOn;

                if (isFlashlightOn) {
                    codeScanner.setFlashEnabled(true);
                    btnFlashlight.setImageResource(R.drawable.ic_flash_on);
                    Toast.makeText(this, "Flashlight ON", Toast.LENGTH_SHORT).show();
                } else {
                    codeScanner.setFlashEnabled(false);
                    btnFlashlight.setImageResource(R.drawable.ic_flash_off);
                    Toast.makeText(this, "Flashlight OFF", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void returnResult(String result) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BARCODE_RESULT, result);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (codeScanner != null) {
            codeScanner.startPreview();
        }
    }

    @Override
    protected void onPause() {
        if (codeScanner != null) {
            // Turn off flashlight when pausing
            if (isFlashlightOn) {
                codeScanner.setFlashEnabled(false);
                isFlashlightOn = false;
            }
            codeScanner.releaseResources();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (codeScanner != null) {
            // Ensure flashlight is off
            if (isFlashlightOn) {
                codeScanner.setFlashEnabled(false);
            }
            codeScanner.releaseResources();
        }
        super.onDestroy();
    }
}
