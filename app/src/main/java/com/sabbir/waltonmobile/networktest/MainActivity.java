package com.sabbir.waltonmobile.networktest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnWifiTest, btnSimNetworkTest;
    private TextView tvWifiSSID, tvSim1Name, tvSim2Name;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize views
        btnWifiTest = findViewById(R.id.btnWifiTest);
        btnSimNetworkTest = findViewById(R.id.btnSimNetworkTest);
        tvWifiSSID = findViewById(R.id.tvWifiSSID);
        tvSim1Name = findViewById(R.id.tvSim1Name);
        tvSim2Name = findViewById(R.id.tvSim2Name);
        

        // Request permissions
        permissionRequest();

        // Set click listeners
        btnWifiTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkWifiStates();
            }
        });

        btnSimNetworkTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkSimCardStates();
            }
        });
    }

    private void permissionRequest() {
        String[] permissions;

        // For Android 10+ (API 29+), we need location permission to get WiFi SSID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        boolean needsPermission = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needsPermission = true;
                break;
            }
        }

        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void checkSimCardStates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {

            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                int simState = telephonyManager.getSimState();

                switch (simState) {
                    case TelephonyManager.SIM_STATE_ABSENT:
                        Toast.makeText(this, "Please insert sim card", Toast.LENGTH_SHORT).show();
                        tvSim1Name.setText("SIM 1: Not Available");
                        tvSim2Name.setText("SIM 2: Not Available");
                        tvSim1Name.setVisibility(View.VISIBLE);
                        tvSim2Name.setVisibility(View.VISIBLE);
                        break;
                    case TelephonyManager.SIM_STATE_READY:
                        Toast.makeText(this, "SIM card is ready", Toast.LENGTH_SHORT).show();
                        getSimCardNames();
                        break;
                    case TelephonyManager.SIM_STATE_UNKNOWN:
                        Toast.makeText(this, "SIM state unknown", Toast.LENGTH_SHORT).show();
                        tvSim1Name.setText("SIM 1: Unknown State");
                        tvSim2Name.setText("SIM 2: Unknown State");
                        tvSim1Name.setVisibility(View.VISIBLE);
                        tvSim2Name.setVisibility(View.VISIBLE);
                        break;
                    default:
                        Toast.makeText(this, "SIM card not ready", Toast.LENGTH_SHORT).show();
                        tvSim1Name.setText("SIM 1: Not Ready");
                        tvSim2Name.setText("SIM 2: Not Ready");
                        tvSim1Name.setVisibility(View.VISIBLE);
                        tvSim2Name.setVisibility(View.VISIBLE);
                        break;
                }
            }
        } else {
            Toast.makeText(this, "Permission required to check SIM status", Toast.LENGTH_SHORT).show();
        }
    }

    private void getSimCardNames() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager subscriptionManager = SubscriptionManager.from(this);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED) {

                    List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();

                    // Initialize both SIM TextViews as not available
                    tvSim1Name.setText("SIM 1: Not Available");
                    tvSim2Name.setText("SIM 2: Not Available");

                    if (subscriptionInfos != null && !subscriptionInfos.isEmpty()) {
                        for (int i = 0; i < subscriptionInfos.size() && i < 2; i++) {
                            SubscriptionInfo info = subscriptionInfos.get(i);
                            String carrierName = info.getCarrierName().toString();
                            String displayName = info.getDisplayName().toString();

                            String simInfo;
                            if (!displayName.isEmpty() && !displayName.equals(carrierName)) {
                                simInfo = displayName + " (" + carrierName + ")";
                            } else {
                                simInfo = carrierName;
                            }

                            if (i == 0) {
                                tvSim1Name.setText("SIM 1: " + simInfo);
                            } else if (i == 1) {
                                tvSim2Name.setText("SIM 2: " + simInfo);
                            }
                        }
                    }

                    // Show both TextViews
                    tvSim1Name.setVisibility(View.VISIBLE);
                    tvSim2Name.setVisibility(View.VISIBLE);
                }
            } else {
                // For older Android versions
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String carrierName = telephonyManager.getNetworkOperatorName();

                if (!carrierName.isEmpty()) {
                    tvSim1Name.setText("SIM 1: " + carrierName);
                    tvSim2Name.setText("SIM 2: Not Available");
                } else {
                    tvSim1Name.setText("SIM 1: Carrier name unavailable");
                    tvSim2Name.setText("SIM 2: Not Available");
                }

                tvSim1Name.setVisibility(View.VISIBLE);
                tvSim2Name.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            tvSim1Name.setText("SIM 1: Error reading info");
            tvSim2Name.setText("SIM 2: Error reading info");
            tvSim1Name.setVisibility(View.VISIBLE);
            tvSim2Name.setVisibility(View.VISIBLE);
        }
    }

    private void checkWifiStates() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            if (wifiManager.isWifiEnabled()) {
                // Check location permission for Android 10+
                boolean hasLocationPermission = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    hasLocationPermission = ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                } else {
                    hasLocationPermission = ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                }

                if (!hasLocationPermission) {
                    Toast.makeText(this, "Location permission required for WiFi SSID", Toast.LENGTH_LONG).show();
                    tvWifiSSID.setText("WiFi SSID: Location permission required");
                    tvWifiSSID.setVisibility(View.VISIBLE);
                    return;
                }

                // Check if connected to WiFi
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (wifiInfo != null && wifiInfo.isConnected()) {
                    WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                    String ssid = connectionInfo.getSSID();

                    if (ssid != null && !ssid.equals("<unknown ssid>") && !ssid.equals("\"\"")) {
                        // Remove quotes from SSID
                        ssid = ssid.replace("\"", "");
                        Toast.makeText(this, "WiFi is connected to: " + ssid, Toast.LENGTH_SHORT).show();
                        tvWifiSSID.setText("WiFi SSID: " + ssid);
                    } else {
                        Toast.makeText(this, "WiFi connected but SSID unavailable", Toast.LENGTH_SHORT).show();
                        tvWifiSSID.setText("WiFi SSID: Connected (Name unavailable)");
                    }
                } else {
                    Toast.makeText(this, "WiFi is ON but not connected", Toast.LENGTH_SHORT).show();
                    tvWifiSSID.setText("WiFi SSID: Not Connected");
                }

                tvWifiSSID.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Please turn on wifi and connect", Toast.LENGTH_SHORT).show();
                tvWifiSSID.setText("WiFi SSID: WiFi is OFF");
                tvWifiSSID.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "WiFi not available", Toast.LENGTH_SHORT).show();
            tvWifiSSID.setText("WiFi SSID: Not Available");
            tvWifiSSID.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions denied. WiFi SSID may not be available.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
