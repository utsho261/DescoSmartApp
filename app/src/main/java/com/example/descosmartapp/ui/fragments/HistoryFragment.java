package com.example.descosmartapp.ui.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.descosmartapp.R;
import com.example.descosmartapp.db.AppDatabase;
import com.example.descosmartapp.db.MeterProfile;
import com.example.descosmartapp.model.RechargeResponse;
import com.example.descosmartapp.pattern.facade.DescoServiceFacade;
import com.example.descosmartapp.ui.adapters.RechargeAdapter;

import java.util.ArrayList;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    RecyclerView recyclerView;
    ProgressBar progress;
    TextView tvEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = v.findViewById(R.id.recyclerHistory);
        progress     = v.findViewById(R.id.progressHistory);
        tvEmpty      = v.findViewById(R.id.tvEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        MeterProfile meter = AppDatabase.getInstance(requireContext()).meterDao().getActiveMeter();
        if (meter != null) loadHistory(meter);

        return v;
    }

    private void loadHistory(MeterProfile meter) {
        progress.setVisibility(View.VISIBLE);

        Calendar cal = Calendar.getInstance();
        String to = formatDate(cal);
        cal.add(Calendar.MONTH, -3);
        String from = formatDate(cal);

        DescoServiceFacade.getInstance().fetchRechargeHistory(meter.accountNo, from, to,
                new Callback<RechargeResponse>() {
                    @Override
                    public void onResponse(Call<RechargeResponse> call, Response<RechargeResponse> response) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().data != null
                                    && !response.body().data.isEmpty()) {
                                recyclerView.setAdapter(new RechargeAdapter(response.body().data));
                                tvEmpty.setVisibility(View.GONE);
                            } else {
                                tvEmpty.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                    @Override
                    public void onFailure(Call<RechargeResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            progress.setVisibility(View.GONE);
                            tvEmpty.setText("লোড হয়নি। পুনরায় চেষ্টা করুন।");
                            tvEmpty.setVisibility(View.VISIBLE);
                        });
                    }
                });
    }

    private String formatDate(Calendar cal) {
        return cal.get(Calendar.YEAR) + "-" +
                String.format("%02d", cal.get(Calendar.MONTH) + 1) + "-" +
                String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
    }
}