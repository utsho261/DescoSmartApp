package com.example.descosmartapp.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.descosmartapp.R;
import com.example.descosmartapp.db.AppDatabase;
import com.example.descosmartapp.db.MeterProfile;
import com.example.descosmartapp.ui.AddMeterActivity;
import com.example.descosmartapp.ui.adapters.MeterManageAdapter;
import com.example.descosmartapp.worker.WorkScheduler;

import java.util.List;

public class SettingsFragment extends Fragment {

    RecyclerView recyclerMeters;
    Button btnAddMeter, btnReschedule;
    AppDatabase db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        recyclerMeters  = v.findViewById(R.id.recyclerMeters);
        btnAddMeter     = v.findViewById(R.id.btnAddNewMeter);
        btnReschedule   = v.findViewById(R.id.btnReschedule);
        db = AppDatabase.getInstance(requireContext());

        recyclerMeters.setLayoutManager(new LinearLayoutManager(requireContext()));
        loadMeters();

        btnAddMeter.setOnClickListener(x ->
                startActivity(new Intent(requireContext(), AddMeterActivity.class)));

        btnReschedule.setOnClickListener(x -> {
            WorkScheduler.scheduleAll(requireContext());
            Toast.makeText(requireContext(), "নোটিফিকেশন পুনরায় সেট হয়েছে", Toast.LENGTH_SHORT).show();
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMeters();
    }

    private void loadMeters() {
        List<MeterProfile> meters = db.meterDao().getAllMeters();
        recyclerMeters.setAdapter(new MeterManageAdapter(meters, requireContext(), db, this::loadMeters));
    }
}