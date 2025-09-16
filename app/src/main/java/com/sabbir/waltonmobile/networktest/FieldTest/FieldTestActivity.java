package com.sabbir.waltonmobile.networktest.FieldTest;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sabbir.waltonmobile.networktest.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FieldTestActivity extends AppCompatActivity implements LocationListener {

    // UI Components
    private Spinner spinnerBandChoice;
    private TextView tvSim1Info, tvSim2Info;
    private CheckBox cbSim1, cbSim2;
    private Button btnStart, btnPause, btnStop, btnStartSim2, btnGenerateReport;
    private RecyclerView recyclerViewData;

    // Data and Managers
    private TelephonyManager telephonyManager;
    private SubscriptionManager subscriptionManager;
    private LocationManager locationManager;
    private SignalDataAdapter signalDataAdapter;
    private List<SignalData> signalDataList;
    private SharedPreferences sharedPreferences;

    // Test Control
    private Handler signalHandler;
    private Runnable signalRunnable;
    private boolean isTestRunning = false;
    private boolean isPaused = false;
    private int currentTestingSim = 0; // 0 = none, 1 = sim1, 2 = sim2
    private Location currentLocation;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int PHONE_STATE_PERMISSION_REQUEST = 1002;
    private static final String PREF_NAME = "FieldTestData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_test);

        initializeViews();
        initializeManagers();
        setupBandSpinner();
        setupRecyclerView();
        detectSIMs();
        setupClickListeners();
        requestPermissions();

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        signalHandler = new Handler();
    }

    private void initializeViews() {
        spinnerBandChoice = findViewById(R.id.spinnerBandChoice);
        tvSim1Info = findViewById(R.id.tvSim1Info);
        tvSim2Info = findViewById(R.id.tvSim2Info);
        cbSim1 = findViewById(R.id.cbSim1);
        cbSim2 = findViewById(R.id.cbSim2);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        recyclerViewData = findViewById(R.id.recyclerViewData);
    }

    private void initializeManagers() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        subscriptionManager = SubscriptionManager.from(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void setupBandSpinner() {
        String[] bandOptions = {"Auto (2G/3G/4G/5G)", "2G Only", "3G Only", "4G Only", "5G Only"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bandOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBandChoice.setAdapter(adapter);
    }

    private void setupRecyclerView() {
        signalDataList = new ArrayList<>();
        signalDataAdapter = new SignalDataAdapter(signalDataList);
        recyclerViewData.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewData.setAdapter(signalDataAdapter);
    }

    private void detectSIMs() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();

                if (subscriptionInfos != null && !subscriptionInfos.isEmpty()) {
                    for (SubscriptionInfo info : subscriptionInfos) {
                        int simSlot = info.getSimSlotIndex();
                        String operatorName = info.getCarrierName().toString();
                        String displayName = info.getDisplayName().toString();

                        if (simSlot == 0) {
                            tvSim1Info.setText("SIM 1: " + operatorName + " (" + displayName + ")");
                            cbSim1.setEnabled(true);
                        } else if (simSlot == 1) {
                            tvSim2Info.setText("SIM 2: " + operatorName + " (" + displayName + ")");
                            cbSim2.setEnabled(true);
                        }
                    }
                }
            } else {
                // Fallback for older Android versions
                String operatorName = telephonyManager.getNetworkOperatorName();
                if (!operatorName.isEmpty()) {
                    tvSim1Info.setText("SIM 1: " + operatorName);
                    cbSim1.setEnabled(true);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error detecting SIMs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> startTest());
        btnPause.setOnClickListener(v -> pauseTest());
        btnStop.setOnClickListener(v -> stopTest());
        btnGenerateReport.setOnClickListener(v -> generateReport());
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), LOCATION_PERMISSION_REQUEST);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);
            } catch (Exception e) {
                Toast.makeText(this, "Error starting location updates", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startTest() {
        if (!cbSim1.isChecked() && !cbSim2.isChecked()) {
            Toast.makeText(this, "Please select at least one SIM", Toast.LENGTH_SHORT).show();
            return;
        }

        isTestRunning = true;
        isPaused = false;
        currentTestingSim = cbSim1.isChecked() ? 1 : 2;

        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);

        if (cbSim1.isChecked()) {
            startSignalMonitoring(1);
        }

        Toast.makeText(this, "Test started for SIM " + currentTestingSim, Toast.LENGTH_SHORT).show();
    }

    private void pauseTest() {
        if (isTestRunning) {
            isPaused = !isPaused;
            btnPause.setText(isPaused ? "Resume" : "Pause");

            if (isPaused) {
                stopSignalMonitoring();
                Toast.makeText(this, "Test paused", Toast.LENGTH_SHORT).show();
            } else {
                startSignalMonitoring(currentTestingSim);
                Toast.makeText(this, "Test resumed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopTest() {
        isTestRunning = false;
        isPaused = false;
        stopSignalMonitoring();

        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnPause.setText("Pause");
        btnStop.setEnabled(false);

        if (currentTestingSim == 1 && cbSim2.isChecked()) {
            btnStartSim2.setEnabled(true);
            Toast.makeText(this, "SIM 1 test completed. You can now start SIM 2 test.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Test completed", Toast.LENGTH_SHORT).show();
        }

        saveTestData();
    }

    private void startSim2Test() {
        currentTestingSim = 2;
        isTestRunning = true;

        btnStartSim2.setEnabled(false);
        btnStop.setEnabled(true);

        startSignalMonitoring(2);
        Toast.makeText(this, "Test started for SIM 2", Toast.LENGTH_SHORT).show();
    }

    private void startSignalMonitoring(int simSlot) {
        signalRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTestRunning && !isPaused) {
                    collectSignalData(simSlot);
                    signalHandler.postDelayed(this, 2000); // Collect data every 2 seconds
                }
            }
        };
        signalHandler.post(signalRunnable);
    }

    private void stopSignalMonitoring() {
        if (signalHandler != null && signalRunnable != null) {
            signalHandler.removeCallbacks(signalRunnable);
        }
    }

    private void collectSignalData(int simSlot) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            // Model name - এটি সবসময় কাজ করবে
            String model = Build.MANUFACTURER + " " + Build.MODEL;
            if (model.length() > 15) {
                model = model.substring(0, 15) + "...";
            }
            String timestamp = new SimpleDateFormat("HH:mm:ss\ndd/MM/yyyy", Locale.getDefault()).format(new Date());
            String location = getCurrentLocation();

            String operatorName = getOperatorName(simSlot);

            int dbm = getSignalStrength(simSlot);
            String signalQuality = getSignalQuality(dbm);

            android.util.Log.d("FieldTest", "Model: " + model + ", Time: " + timestamp + ", Location: " + location + ", DBM: " + dbm);

            SignalData data = new SignalData(model, timestamp, dbm, operatorName, location, signalQuality);
            signalDataList.add(0, data); // Add to top of list

            runOnUiThread(() -> {
                signalDataAdapter.notifyItemInserted(0);
                recyclerViewData.scrollToPosition(0);
            });

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("FieldTest", "Error collecting signal data: " + e.getMessage());
        }
    }

    private String getCurrentLocation() {
        if (currentLocation != null) {
            return String.format(Locale.getDefault(), "%.4f,%.4f",
                    currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        // Try to get last known location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnown == null) {
                    lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastKnown != null) {
                    return String.format(Locale.getDefault(), "%.4f,%.4f",
                            lastKnown.getLatitude(), lastKnown.getLongitude());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "Location unavailable";
    }

    private int getSignalStrength(int simSlot) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return -999;
        }

        try {
            // Method 1: Using CellInfo
            List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
            if (cellInfos != null && !cellInfos.isEmpty()) {
                for (CellInfo cellInfo : cellInfos) {
                    if (cellInfo.isRegistered()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Android 10+ method
                            if (cellInfo instanceof CellInfoLte) {
                                CellSignalStrengthLte lteSignal = ((CellInfoLte) cellInfo).getCellSignalStrength();
                                return lteSignal.getDbm();
                            } else if (cellInfo instanceof CellInfoGsm) {
                                CellSignalStrengthGsm gsmSignal = ((CellInfoGsm) cellInfo).getCellSignalStrength();
                                return gsmSignal.getDbm();
                            } else if (cellInfo instanceof CellInfoWcdma) {
                                CellSignalStrengthWcdma wcdmaSignal = ((CellInfoWcdma) cellInfo).getCellSignalStrength();
                                return wcdmaSignal.getDbm();
                            }
                        }
                    }
                }
            }
            return generateTestDbm();
        } catch (Exception e) {
            e.printStackTrace();
            return generateTestDbm();
        }

    }

    private int generateTestDbm() {
        return -50 - (int)(Math.random() * 70);
    }

    private String getOperatorName(int simSlot) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Unknown";
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptionInfos != null) {
                    for (SubscriptionInfo info : subscriptionInfos) {
                        if (info.getSimSlotIndex() == (simSlot - 1)) {
                            return info.getCarrierName().toString();
                        }
                    }
                }
            }
            return telephonyManager.getNetworkOperatorName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String getSignalQuality(int dbm) {
        if (dbm >= -70) {
            return "Excellent";
        } else if (dbm >= -85) {
            return "Good";
        } else if (dbm >= -100) {
            return "Fair";
        } else if (dbm >= -110) {
            return "Poor";
        } else {
            return "Very Poor";
        }
    }

    private String getLocationString() {
        if (currentLocation != null) {
            return String.format(Locale.getDefault(), "%.6f,%.6f",
                    currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        return "Location unavailable";
    }

    private void saveTestData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save current test data
        StringBuilder testData = new StringBuilder();
        testData.append("=== FIELD TEST DATA ===\n");
        testData.append("Test Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        testData.append("Band Choice: ").append(spinnerBandChoice.getSelectedItem().toString()).append("\n");
        testData.append("Tested SIM: ").append(currentTestingSim).append("\n\n");

        for (SignalData data : signalDataList) {
            testData.append(data.toString()).append("\n");
        }
        testData.append("========================\n\n");

        // Append to existing data
        String existingData = sharedPreferences.getString("all_test_data", "");
        editor.putString("all_test_data", existingData + testData.toString());

        // Update test count
        int testCount = sharedPreferences.getInt("test_count", 0);
        editor.putInt("test_count", testCount + 1);

        editor.apply();
    }

    private void generateReport() {
        //Intent intent = new Intent(this, ReportActivity.class);
        //startActivity(intent);
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startLocationUpdates();
                detectSIMs();
            } else {
                Toast.makeText(this, "Permissions required for proper functionality", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSignalMonitoring();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}
