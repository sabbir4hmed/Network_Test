package com.sabbir.waltonbd.wcmsproductchecker;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2500; // 2.5 seconds
    private TextView tvLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        tvLoading = findViewById(R.id.tvLoading);

        // Hide action bar if exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Check internet connection after delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkInternetAndProceed();
            }
        }, SPLASH_DELAY);
    }

    private void checkInternetAndProceed() {
        if (isInternetAvailable()) {
            // Update loading text
            if (tvLoading != null) {
                tvLoading.setText("Connecting...");
            }

            // Navigate to Login Activity
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();

            // Add transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            // Update loading text
            if (tvLoading != null) {
                tvLoading.setText("No Internet Connection");
            }

            // Show toast message
            Toast.makeText(SplashActivity.this,
                    getString(R.string.please_connect_internet),
                    Toast.LENGTH_LONG).show();

            // Retry after 2 seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (tvLoading != null) {
                        tvLoading.setText("Retrying...");
                    }
                    checkInternetAndProceed();
                }
            }, 2000);
        }
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
}
