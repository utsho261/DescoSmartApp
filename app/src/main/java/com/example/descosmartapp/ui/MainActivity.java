package com.example.descosmartapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.descosmartapp.R;
import com.example.descosmartapp.db.AppDatabase;
import com.example.descosmartapp.db.MeterProfile;
import com.example.descosmartapp.ui.fragments.DashboardFragment;
import com.example.descosmartapp.ui.fragments.HistoryFragment;
import com.example.descosmartapp.ui.fragments.ReportsFragment;
import com.example.descosmartapp.ui.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    TextView tvActiveMeter;
    ImageButton btnSwitchMeter;
    AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);

        tvActiveMeter  = findViewById(R.id.tvActiveMeter);
        btnSwitchMeter = findViewById(R.id.btnSwitchMeter);
        bottomNav      = findViewById(R.id.bottomNav);

        requestNotificationPermission();
        updateMeterHeader();

        btnSwitchMeter.setOnClickListener(v -> showMeterSwitchDialog());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_dashboard)  fragment = new DashboardFragment();
            else if (id == R.id.nav_history) fragment = new HistoryFragment();
            else if (id == R.id.nav_reports) fragment = new ReportsFragment();
            else fragment = new SettingsFragment();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        });

        // Default tab
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new DashboardFragment())
                .commit();
    }

    private void updateMeterHeader() {
        MeterProfile active = db.meterDao().getActiveMeter();
        if (active != null) {
            tvActiveMeter.setText(active.label + "  (" + active.accountNo + ")");
        }
    }

    private void showMeterSwitchDialog() {
        List<MeterProfile> meters = db.meterDao().getAllMeters();
        String[] labels = new String[meters.size() + 1];
        for (int i = 0; i < meters.size(); i++) {
            labels[i] = meters.get(i).label + " — " + meters.get(i).accountNo;
        }
        labels[meters.size()] = "+ নতুন মিটার যোগ করুন";

        new AlertDialog.Builder(this)
                .setTitle("মিটার পরিবর্তন করুন")
                .setItems(labels, (dialog, which) -> {
                    if (which == meters.size()) {
                        startActivity(new Intent(this, AddMeterActivity.class));
                    } else {
                        MeterProfile selected = meters.get(which);
                        db.meterDao().deactivateAll();
                        db.meterDao().setActive(selected.id);
                        updateMeterHeader();
                        // Reload dashboard
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragmentContainer, new DashboardFragment())
                                .commit();
                        bottomNav.setSelectedItemId(R.id.nav_dashboard);
                        Toast.makeText(this, selected.label + " সিলেক্ট হয়েছে", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMeterHeader();
    }
}