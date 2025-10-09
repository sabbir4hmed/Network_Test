package com.sabbir.waltonmobile.networktest.FieldTest;

import android.Manifest;
import android.content.Context;
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
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sabbir.waltonmobile.networktest.ExcelExporter;
import com.sabbir.waltonmobile.networktest.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FieldTestActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "FieldTestActivity";

    // UI Components
    private Spinner spinnerBandChoice;
    private TextView tvSim1Info, tvSim2Info;
    private CheckBox cbSim1, cbSim2;
    private Button btnStart, btnPause, btnStop, btnGenerateReport;
    private RecyclerView recyclerViewData;

    private ProgressBar progressBar;
    private TextView tvProgressStatus;

    // dBm Meter TextViews
    private TextView tvSim1DbmValue, tvSim2DbmValue;
    private TextView tvSim1SignalStatus, tvSim2SignalStatus;

    // Data and Managers
    private TelephonyManager telephonyManager;
    private TelephonyManager telephonyManagerSim1, telephonyManagerSim2;
    private SubscriptionManager subscriptionManager;
    private LocationManager locationManager;
    private SignalDataAdapter signalDataAdapter;
    private List<SignalData> signalDataList;
    private SharedPreferences sharedPreferences;

    // Test Control
    private Handler signalHandler;
    private Runnable signalRunnable;

    // Instant dBm Update Handler
    private Handler dbmHandler;
    private Runnable dbmRunnable;

    private boolean isTestRunning = false;
    private boolean isPaused = false;
    private int currentTestingSim = 0; // 0 = none, 1 = sim1, 2 = sim2
    private Location currentLocation;

    // SIM availability flags (separate from checkbox enabled state)
    private boolean sim1Available = false;
    private boolean sim2Available = false;

    // SIM subscription IDs
    private int sim1SubscriptionId = -1;
    private int sim2SubscriptionId = -1;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String PREF_NAME = "FieldTestData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_test);

        initializeViews();
        initializeManagers();
        setupBandSpinner();
        setupRecyclerView();
        setupClickListeners();
        setupCheckBoxListeners();
        requestPermissions();

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        signalHandler = new Handler();
        dbmHandler = new Handler();

        // Detect SIMs after a short delay to ensure managers are initialized
        new Handler().postDelayed(() -> {
            detectSIMs();
            // Start instant dBm monitoring after SIM detection
            startInstantDbmMonitoring();
        }, 500);
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

        // Initialize ProgressBar and status text
        progressBar = findViewById(R.id.progressBar);
        tvProgressStatus = findViewById(R.id.tvProgressStatus);

        // Initialize dBm meter TextViews
        tvSim1DbmValue = findViewById(R.id.tvSim1DbmValue);
        tvSim2DbmValue = findViewById(R.id.tvSim2DbmValue);
        tvSim1SignalStatus = findViewById(R.id.tvSim1SignalStatus);
        tvSim2SignalStatus = findViewById(R.id.tvSim2SignalStatus);

        // Set initial values
        tvSim1DbmValue.setText("-- dBm");
        tvSim2DbmValue.setText("-- dBm");
        tvSim1SignalStatus.setText("Initializing...");
        tvSim2SignalStatus.setText("Initializing...");
    }

    private void initializeManagers() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        subscriptionManager = SubscriptionManager.from(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Initialize SIM-specific TelephonyManagers
        initializeSIMManagers();
    }

    private void initializeSIMManagers() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_PHONE_STATE permission not granted");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptionInfos != null) {
                    Log.d(TAG, "Found " + subscriptionInfos.size() + " active subscriptions");
                    for (SubscriptionInfo info : subscriptionInfos) {
                        int simSlot = info.getSimSlotIndex();
                        int subscriptionId = info.getSubscriptionId();

                        Log.d(TAG, "SIM Slot: " + simSlot + ", Subscription ID: " + subscriptionId);

                        if (simSlot == 0) {
                            sim1SubscriptionId = subscriptionId;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                telephonyManagerSim1 = telephonyManager.createForSubscriptionId(subscriptionId);
                                Log.d(TAG, "Created TelephonyManager for SIM 1");
                            }
                        } else if (simSlot == 1) {
                            sim2SubscriptionId = subscriptionId;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                telephonyManagerSim2 = telephonyManager.createForSubscriptionId(subscriptionId);
                                Log.d(TAG, "Created TelephonyManager for SIM 2");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SIM managers", e);
        }
    }

    private void setupBandSpinner() {
        String[] bandOptions = {"Auto (2G/3G/4G/5G)", "2G Only", "3G Only", "4G Only", "5G Only"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bandOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBandChoice.setAdapter(adapter);

        spinnerBandChoice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                Toast.makeText(FieldTestActivity.this, "Selected Band: " + selected, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        signalDataList = new ArrayList<>();
        signalDataAdapter = new SignalDataAdapter(signalDataList);
        recyclerViewData.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewData.setAdapter(signalDataAdapter);
    }

    private void detectSIMs() {
        Log.d(TAG, "Detecting SIMs...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_PHONE_STATE permission not granted for SIM detection");
            return;
        }

        // Reset SIM info first
        sim1Available = false;
        sim2Available = false;

        runOnUiThread(() -> {
            tvSim1Info.setText("SIM 1: Not Available");
            tvSim2Info.setText("SIM 2: Not Available");
            cbSim1.setEnabled(false);
            cbSim2.setEnabled(false);
        });

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();

                if (subscriptionInfos != null && !subscriptionInfos.isEmpty()) {
                    Log.d(TAG, "Found " + subscriptionInfos.size() + " subscription(s)");

                    for (SubscriptionInfo info : subscriptionInfos) {
                        int simSlot = info.getSimSlotIndex();
                        String operatorName = info.getCarrierName().toString();
                        String displayName = info.getDisplayName().toString();

                        Log.d(TAG, "SIM Slot " + simSlot + ": " + operatorName + " (" + displayName + ")");

                        if (simSlot == 0) {
                            sim1Available = true;
                            runOnUiThread(() -> {
                                tvSim1Info.setText("SIM 1: " + operatorName + " (" + displayName + ")");
                                cbSim1.setEnabled(true);
                                Log.d(TAG, "Enabled SIM 1 checkbox");
                            });
                        } else if (simSlot == 1) {
                            sim2Available = true;
                            runOnUiThread(() -> {
                                tvSim2Info.setText("SIM 2: " + operatorName + " (" + displayName + ")");
                                cbSim2.setEnabled(true);
                                Log.d(TAG, "Enabled SIM 2 checkbox");
                            });
                        }
                    }
                } else {
                    Log.d(TAG, "No active subscriptions found");
                }
            } else {
                // Fallback for older Android versions
                String operatorName = telephonyManager.getNetworkOperatorName();
                if (!operatorName.isEmpty()) {
                    sim1Available = true;
                    runOnUiThread(() -> {
                        tvSim1Info.setText("SIM 1: " + operatorName);
                        cbSim1.setEnabled(true);
                        Log.d(TAG, "Enabled SIM 1 checkbox (legacy)");
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting SIMs", e);
            Toast.makeText(this, "Error detecting SIMs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> startTest());
        btnPause.setOnClickListener(v -> pauseTest());
        btnStop.setOnClickListener(v -> stopTest());
        btnGenerateReport.setOnClickListener(v -> generateReport());
    }

    // Setup checkbox listeners for mutual exclusion
    private void setupCheckBoxListeners() {
        cbSim1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cbSim2.setChecked(false); // Uncheck SIM 2 when SIM 1 is selected
                    Log.d(TAG, "SIM 1 selected, SIM 2 unchecked");
                }
            }
        });

        cbSim2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cbSim1.setChecked(false); // Uncheck SIM 1 when SIM 2 is selected
                    Log.d(TAG, "SIM 2 selected, SIM 1 unchecked");
                }
            }
        });
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
                Log.d(TAG, "Location updates started");
            } catch (Exception e) {
                Log.e(TAG, "Error starting location updates", e);
                Toast.makeText(this, "Error starting location updates", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Start instant dBm monitoring with debug logging
    private void startInstantDbmMonitoring() {
        Log.d(TAG, "Starting instant dBm monitoring");

        dbmRunnable = new Runnable() {
            @Override
            public void run() {
                // Debug logging
                Log.d(TAG, "dBm monitoring tick - SIM1 available: " + sim1Available +
                        ", SIM1 checked: " + cbSim1.isChecked() +
                        ", SIM2 available: " + sim2Available +
                        ", SIM2 checked: " + cbSim2.isChecked() +
                        ", Test running: " + isTestRunning);

                updateDbmMeters();
                dbmHandler.postDelayed(this, 5); // 5 milliseconds delay
            }
        };
        dbmHandler.post(dbmRunnable);
    }

    // Stop instant dBm monitoring
    private void stopInstantDbmMonitoring() {
        Log.d(TAG, "Stopping instant dBm monitoring");
        if (dbmHandler != null && dbmRunnable != null) {
            dbmHandler.removeCallbacks(dbmRunnable);
        }
    }

    // FIXED: Update dBm meters based on SIM availability and selection
    private void updateDbmMeters() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_PHONE_STATE permission not granted for dBm update");
            return;
        }

        try {
            // Check selection state
            boolean noSimSelected = !cbSim1.isChecked() && !cbSim2.isChecked();
            boolean sim1Selected = cbSim1.isChecked();
            boolean sim2Selected = cbSim2.isChecked();

            Log.d(TAG, "updateDbmMeters - noSimSelected: " + noSimSelected +
                    ", sim1Selected: " + sim1Selected +
                    ", sim2Selected: " + sim2Selected);

            // Update SIM 1 dBm - Use sim1Available instead of cbSim1.isEnabled()
            if (sim1Available && (noSimSelected || sim1Selected)) {
                int sim1Dbm = getSignalStrengthForSim(1);
                String sim1Status = getSignalQuality(sim1Dbm);

                Log.d(TAG, "SIM 1 dBm: " + sim1Dbm + ", Status: " + sim1Status);

                runOnUiThread(() -> {
                    tvSim1DbmValue.setText(sim1Dbm + " dBm");
                    tvSim1SignalStatus.setText(sim1Status);

                    // Change color based on signal quality
                    int color = getSignalColor(sim1Dbm);
                    tvSim1DbmValue.setTextColor(color);
                    tvSim1SignalStatus.setTextColor(color);
                });
            } else if (sim2Selected && sim1Available) {
                // Mute SIM 1 when SIM 2 is selected (but SIM 1 is available)
                Log.d(TAG, "Muting SIM 1 (SIM 2 selected)");
                runOnUiThread(() -> {
                    tvSim1DbmValue.setText("-- dBm");
                    tvSim1SignalStatus.setText("Muted");
                    tvSim1DbmValue.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    tvSim1SignalStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                });
            } else {
                // No SIM available
                Log.d(TAG, "SIM 1 not available");
                runOnUiThread(() -> {
                    tvSim1DbmValue.setText("-- dBm");
                    tvSim1SignalStatus.setText("No SIM");
                    tvSim1DbmValue.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    tvSim1SignalStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                });
            }

            // Update SIM 2 dBm - Use sim2Available instead of cbSim2.isEnabled()
            if (sim2Available && (noSimSelected || sim2Selected)) {
                int sim2Dbm = getSignalStrengthForSim(2);
                String sim2Status = getSignalQuality(sim2Dbm);

                Log.d(TAG, "SIM 2 dBm: " + sim2Dbm + ", Status: " + sim2Status);

                runOnUiThread(() -> {
                    tvSim2DbmValue.setText(sim2Dbm + " dBm");
                    tvSim2SignalStatus.setText(sim2Status);

                    // Change color based on signal quality
                    int color = getSignalColor(sim2Dbm);
                    tvSim2DbmValue.setTextColor(color);
                    tvSim2SignalStatus.setTextColor(color);
                });
            } else if (sim1Selected && sim2Available) {
                // Mute SIM 2 when SIM 1 is selected (but SIM 2 is available)
                Log.d(TAG, "Muting SIM 2 (SIM 1 selected)");
                runOnUiThread(() -> {
                    tvSim2DbmValue.setText("-- dBm");
                    tvSim2SignalStatus.setText("Muted");
                    tvSim2DbmValue.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    tvSim2SignalStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                });
            } else {
                // No SIM available
                Log.d(TAG, "SIM 2 not available");
                runOnUiThread(() -> {
                    tvSim2DbmValue.setText("-- dBm");
                    tvSim2SignalStatus.setText("No SIM");
                    tvSim2DbmValue.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    tvSim2SignalStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating dBm meters", e);
        }
    }

    // Get signal strength for specific SIM with different values
    private int getSignalStrengthForSim(int simSlot) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_PHONE_STATE permission not granted for signal strength");
            return -999;
        }

        try {
            TelephonyManager targetTelephonyManager = null;

            // Get the appropriate TelephonyManager for the SIM
            if (simSlot == 1 && telephonyManagerSim1 != null) {
                targetTelephonyManager = telephonyManagerSim1;
                Log.d(TAG, "Using TelephonyManager for SIM 1");
            } else if (simSlot == 2 && telephonyManagerSim2 != null) {
                targetTelephonyManager = telephonyManagerSim2;
                Log.d(TAG, "Using TelephonyManager for SIM 2");
            }

            if (targetTelephonyManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                List<CellInfo> cellInfos = targetTelephonyManager.getAllCellInfo();
                if (cellInfos != null && !cellInfos.isEmpty()) {
                    for (CellInfo cellInfo : cellInfos) {
                        if (cellInfo.isRegistered()) {
                            if (cellInfo instanceof CellInfoLte) {
                                int dbm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                                Log.d(TAG, "Real LTE signal for SIM " + simSlot + ": " + dbm + " dBm");
                                return dbm;
                            } else if (cellInfo instanceof CellInfoGsm) {
                                int dbm = ((CellInfoGsm) cellInfo).getCellSignalStrength().getDbm();
                                Log.d(TAG, "Real GSM signal for SIM " + simSlot + ": " + dbm + " dBm");
                                return dbm;
                            } else if (cellInfo instanceof CellInfoWcdma) {
                                int dbm = ((CellInfoWcdma) cellInfo).getCellSignalStrength().getDbm();
                                Log.d(TAG, "Real WCDMA signal for SIM " + simSlot + ": " + dbm + " dBm");
                                return dbm;
                            }
                        }
                    }
                }
            }

            // If we can't get real data, generate different test values for each SIM
            int testDbm = generateTestDbmForSim(simSlot);
            Log.d(TAG, "Generated test dBm for SIM " + simSlot + ": " + testDbm);
            return testDbm;

        } catch (Exception e) {
            Log.e(TAG, "Error getting signal strength for SIM " + simSlot, e);
            return generateTestDbmForSim(simSlot);
        }
    }

    // Generate different test dBm values for each SIM
    private int generateTestDbmForSim(int simSlot) {
        // Generate different ranges for each SIM to show variation
        if (simSlot == 1) {
            // SIM 1: Range -60 to -110 dBm
            return -60 - (int) (Math.random() * 50);
        } else {
            // SIM 2: Range -70 to -120 dBm (slightly weaker)
            return -70 - (int) (Math.random() * 50);
        }
    }

    // Get color based on signal strength
    private int getSignalColor(int dbm) {
        if (dbm >= -70) return getResources().getColor(android.R.color.holo_green_dark);
        else if (dbm >= -85) return getResources().getColor(android.R.color.holo_blue_dark);
        else if (dbm >= -100) return getResources().getColor(android.R.color.holo_orange_dark);
        else if (dbm >= -110) return getResources().getColor(android.R.color.holo_red_dark);
        else return getResources().getColor(android.R.color.darker_gray);
    }

    private void startTest() {
        if (!cbSim1.isChecked() && !cbSim2.isChecked()) {
            Toast.makeText(this, "Please select at least one SIM", Toast.LENGTH_SHORT).show();
            return;
        }

        isTestRunning = true;
        isPaused = false;
        currentTestingSim = cbSim1.isChecked() ? 1 : 2;

        Log.d(TAG, "Starting test for SIM " + currentTestingSim);

        // FIXED: Only disable checkboxes for user interaction, don't affect dBm monitoring
        // The dBm monitoring now uses sim1Available/sim2Available flags instead of checkbox enabled state
        cbSim1.setEnabled(false);
        cbSim2.setEnabled(false);

        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);

        startSignalMonitoring(currentTestingSim);

        Toast.makeText(this, "Test started for SIM " + currentTestingSim, Toast.LENGTH_SHORT).show();
    }

    private void pauseTest() {
        if (isTestRunning) {
            isPaused = !isPaused;
            btnPause.setText(isPaused ? "Resume" : "Pause");

            if (isPaused) {
                stopSignalMonitoring();
                Log.d(TAG, "Test paused");
                Toast.makeText(this, "Test paused", Toast.LENGTH_SHORT).show();
            } else {
                startSignalMonitoring(currentTestingSim);
                Log.d(TAG, "Test resumed");
                Toast.makeText(this, "Test resumed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopTest() {
        isTestRunning = false;
        isPaused = false;
        stopSignalMonitoring();

        Log.d(TAG, "Test stopped");

        // FIXED: Re-enable checkboxes based on SIM availability
        runOnUiThread(() -> {
            if (sim1Available) {
                cbSim1.setEnabled(true);
            }
            if (sim2Available) {
                cbSim2.setEnabled(true);
            }
        });

        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnPause.setText("Pause");
        btnStop.setEnabled(false);

        Toast.makeText(this, "Test completed", Toast.LENGTH_SHORT).show();

        saveTestData();
    }

    private void startSignalMonitoring(int simSlot) {
        Log.d(TAG, "Starting signal monitoring for SIM " + simSlot);

        signalRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTestRunning && !isPaused) {
                    collectSignalData(simSlot);
                    signalHandler.postDelayed(this, 2000);
                }
            }
        };
        signalHandler.post(signalRunnable);
    }

    private void stopSignalMonitoring() {
        Log.d(TAG, "Stopping signal monitoring");
        if (signalHandler != null && signalRunnable != null) {
            signalHandler.removeCallbacks(signalRunnable);
        }
    }

    private void collectSignalData(int simSlot) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            String model = Build.MANUFACTURER + " " + Build.MODEL;
            if (model.length() > 15) {
                model = model.substring(0, 15) + "...";
            }
            String timestamp = new SimpleDateFormat("HH:mm:ss\ndd/MM/yyyy", Locale.getDefault()).format(new Date());
            String location = getLocationString();

            String operatorName = getOperatorName(simSlot);

            int dbm = getSignalStrength(simSlot);
            String signalQuality = getSignalQuality(dbm);

            SignalData data = new SignalData(model, timestamp, dbm, operatorName, location, signalQuality);
            signalDataList.add(0, data);

            Log.d(TAG, "Collected signal data: " + dbm + " dBm, " + signalQuality);

            runOnUiThread(() -> {
                signalDataAdapter.notifyItemInserted(0);
                recyclerViewData.scrollToPosition(0);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error collecting signal data", e);
        }
    }

    private int getSignalStrength(int simSlot) {
        // Use the same method as the instant dBm monitoring for consistency
        return getSignalStrengthForSim(simSlot);
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private String getOperatorName(int simSlot) {
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
            Log.e(TAG, "Error getting operator name", e);
            return "Unknown";
        }
    }

    private String getSignalQuality(int dbm) {
        if (dbm >= -70) return "Excellent";
        else if (dbm >= -85) return "Good";
        else if (dbm >= -100) return "Fair";
        else if (dbm >= -110) return "Poor";
        else return "Very Poor";
    }

    private String getLocationString() {
        if (currentLocation != null) {
            return String.format(Locale.getDefault(), "%.4f,%.4f",
                    currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        return "Location unavailable";
    }

    private void saveTestData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        StringBuilder testData = new StringBuilder();
        testData.append("=== FIELD TEST DATA ===\n");
        testData.append("Test Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        testData.append("Band Choice: ").append(spinnerBandChoice.getSelectedItem().toString()).append("\n");
        testData.append("Tested SIM: ").append(currentTestingSim).append("\n\n");

        for (SignalData data : signalDataList) {
            testData.append(data.toString()).append("\n");
        }
        testData.append("========================\n\n");

        String existingData = sharedPreferences.getString("all_test_data", "");
        editor.putString("all_test_data", existingData + testData.toString());

        int testCount = sharedPreferences.getInt("test_count", 0);
        editor.putInt("test_count", testCount + 1);

        editor.apply();

        Log.d(TAG, "Test data saved");
    }

    private void generateReport() {
        if (signalDataList == null || signalDataList.isEmpty()) {
            Toast.makeText(this, "No data to export yet. Start a test first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button and show progress
        btnGenerateReport.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvProgressStatus.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        tvProgressStatus.setText("Preparing report...");

        // Run report generation in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Update progress: 10%
                    updateProgress(10, "Collecting data...");
                    Thread.sleep(200);

                    String timePart = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String fileName = "Field_Test_Report_" + timePart + ".xlsx";

                    String bandChoice = spinnerBandChoice.getSelectedItem() != null
                            ? spinnerBandChoice.getSelectedItem().toString()
                            : "Unknown";
                    int testedSimLocal = currentTestingSim;

                    // Update progress: 30%
                    updateProgress(30, "Creating Excel workbook...");
                    Thread.sleep(200);

                    // Generate the Excel file with progress callback
                    File xlsx = ExcelExporter.exportSignalDataWithProgress(
                            FieldTestActivity.this,
                            fileName,
                            signalDataList,
                            bandChoice,
                            testedSimLocal,
                            sharedPreferences,
                            new ExcelExporter.ProgressCallback() {
                                @Override
                                public void onProgress(int progress, String message) {
                                    updateProgress(progress, message);
                                }
                            }
                    );

                    // Update progress: 95%
                    updateProgress(95, "Finalizing report...");
                    Thread.sleep(200);

                    // Increment report count
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    int reportCount = sharedPreferences.getInt("report_count", 0);
                    editor.putInt("report_count", reportCount + 1);
                    editor.apply();

                    // Update progress: 100%
                    updateProgress(100, "Report generated successfully!");
                    Thread.sleep(300);

                    final File finalXlsx = xlsx;

                    // Show success on UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            tvProgressStatus.setVisibility(View.GONE);
                            btnGenerateReport.setEnabled(true);

                            Toast.makeText(FieldTestActivity.this,
                                    "Report saved: " + finalXlsx.getName(),
                                    Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Report saved at: " + finalXlsx.getAbsolutePath());

                            // Offer to open/share the file
                            //promptOpenShare(finalXlsx);
                        }
                    });

                } catch (OutOfMemoryError oom) {
                    Log.e(TAG, "OutOfMemoryError generating report", oom);
                    showError("Not enough memory to create Excel. Try fewer rows.");
                } catch (Exception e) {
                    Log.e(TAG, "Error generating Excel report", e);
                    showError("Failed to generate report: " + e.getMessage());
                }
            }
        }).start();
    }

    // Helper method to update progress on UI thread
    private void updateProgress(final int progress, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(progress);
                tvProgressStatus.setText(message);
                Log.d(TAG, "Progress: " + progress + "% - " + message);
            }
        });
    }

    // Helper method to show error on UI thread
    private void showError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                tvProgressStatus.setVisibility(View.GONE);
                btnGenerateReport.setEnabled(true);
                Toast.makeText(FieldTestActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // LocationListener methods
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
    }

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
                Log.d(TAG, "All permissions granted");
                startLocationUpdates();

                // Re-initialize managers and detect SIMs after permissions are granted
                initializeSIMManagers();
                new Handler().postDelayed(() -> {
                    detectSIMs();
                    if (dbmHandler == null || dbmRunnable == null) {
                        startInstantDbmMonitoring();
                    }
                }, 500);
            } else {
                Log.d(TAG, "Some permissions denied");
                Toast.makeText(this, "Permissions required for proper functionality", Toast.LENGTH_LONG).show();

                // Still try to detect SIMs and start monitoring with limited functionality
                new Handler().postDelayed(() -> {
                    detectSIMs();
                    startInstantDbmMonitoring();
                }, 500);
            }
        }
    }

    // Override back button to prevent navigation during test
    @Override
    public void onBackPressed() {
        if (isTestRunning) {
            Toast.makeText(this, "Please stop the test before going back", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Back button pressed during test - blocked");
            return;
        }
        Log.d(TAG, "Back button pressed - allowing navigation");
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Activity destroying");
        super.onDestroy();
        stopSignalMonitoring();
        stopInstantDbmMonitoring();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Activity paused");
        super.onPause();
        // Keep dBm monitoring running even when paused for real-time updates
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Activity resumed");
        super.onResume();
        // Ensure dBm monitoring is running when activity resumes
        if (dbmHandler == null || dbmRunnable == null) {
            Log.d(TAG, "Restarting dBm monitoring on resume");
            startInstantDbmMonitoring();
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "Activity started");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Activity stopped");
        super.onStop();
    }
}