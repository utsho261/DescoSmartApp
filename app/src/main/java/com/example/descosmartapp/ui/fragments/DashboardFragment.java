package com.example.descosmartapp.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.descosmartapp.R;
import com.example.descosmartapp.db.AppDatabase;
import com.example.descosmartapp.db.MeterProfile;
import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.model.CustomerResponse;
import com.example.descosmartapp.model.MonthlyResponse;
import com.example.descosmartapp.pattern.adapter.CustomerApiAdapter;
import com.example.descosmartapp.pattern.facade.DescoServiceFacade;
import com.example.descosmartapp.pattern.state.MeterState;
import com.example.descosmartapp.pattern.state.MeterStateFactory;
import com.example.descosmartapp.pattern.strategy.ResidentialBillingStrategy;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    TextView tvName, tvAccount, tvMeter, tvPhone, tvAddress, tvFeeder, tvPhase;
    TextView tvBalance, tvUsage, tvReading, tvStateLabel, tvEstBill;
    TextView tvAlertBanner;
    CardView cardBalanceState, cardAlert;
    ProgressBar progressDash;
    Button btnRefresh;
    BarChart barChart;

    AppDatabase db;
    MeterProfile activeMeter;
    CustomerApiAdapter.UnifiedCustomerModel currentModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        bindViews(v);
        db = AppDatabase.getInstance(requireContext());
        activeMeter = db.meterDao().getActiveMeter();

        if (activeMeter != null) loadAll();

        btnRefresh.setOnClickListener(x -> {
            activeMeter = db.meterDao().getActiveMeter();
            if (activeMeter != null) loadAll();
        });

        return v;
    }

    private void bindViews(View v) {
        tvName        = v.findViewById(R.id.tvName);
        tvAccount     = v.findViewById(R.id.tvAccount);
        tvMeter       = v.findViewById(R.id.tvMeter);
        tvPhone       = v.findViewById(R.id.tvPhone);
        tvAddress     = v.findViewById(R.id.tvAddress);
        tvFeeder      = v.findViewById(R.id.tvFeeder);
        tvPhase       = v.findViewById(R.id.tvPhase);
        tvBalance     = v.findViewById(R.id.tvBalance);
        tvUsage       = v.findViewById(R.id.tvUsage);
        tvReading     = v.findViewById(R.id.tvReading);
        tvStateLabel  = v.findViewById(R.id.tvStateLabel);
        tvEstBill     = v.findViewById(R.id.tvEstBill);
        tvAlertBanner = v.findViewById(R.id.tvAlertBanner);
        cardBalanceState = v.findViewById(R.id.cardBalanceState);
        cardAlert     = v.findViewById(R.id.cardAlert);
        progressDash  = v.findViewById(R.id.progressDash);
        btnRefresh    = v.findViewById(R.id.btnRefresh);
        barChart      = v.findViewById(R.id.barChart);
    }

    private void loadAll() {
        progressDash.setVisibility(View.VISIBLE);
        cardAlert.setVisibility(View.GONE);

        final CustomerApiAdapter.UnifiedCustomerModel[] model = {new CustomerApiAdapter.UnifiedCustomerModel()};
        final int[] done = {0};

        Runnable checkDone = () -> {
            done[0]++;
            if (done[0] >= 2) {
                progressDash.setVisibility(View.GONE);
                currentModel = model[0];
                updateUI(model[0]);
            }
        };

        DescoServiceFacade.getInstance().fetchCustomerByAccount(activeMeter.accountNo, new Callback<CustomerResponse>() {
            @Override
            public void onResponse(Call<CustomerResponse> call, Response<CustomerResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    model[0] = CustomerApiAdapter.adapt(response.body().data, null);
                }
                requireActivity().runOnUiThread(checkDone);
            }
            @Override public void onFailure(Call<CustomerResponse> call, Throwable t) {
                requireActivity().runOnUiThread(checkDone);
            }
        });

        DescoServiceFacade.getInstance().fetchBalance(activeMeter.accountNo, new Callback<BalanceResponse>() {
            @Override
            public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    BalanceResponse.Balance b = response.body().data;
                    model[0].balance      = b.balance;
                    model[0].monthlyUsage = b.currentMonthConsumption;
                    model[0].lastReading  = b.readingTime != null ? b.readingTime : "N/A";
                }
                requireActivity().runOnUiThread(checkDone);
            }
            @Override public void onFailure(Call<BalanceResponse> call, Throwable t) {
                requireActivity().runOnUiThread(checkDone);
            }
        });

        loadMonthlyChart();
    }

    private void updateUI(CustomerApiAdapter.UnifiedCustomerModel m) {
        tvName.setText(m.displayName);
        tvAccount.setText("Account: " + m.accountNo);
        tvMeter.setText("Meter: " + m.meterNo);
        tvPhone.setText("📞 " + m.phone);
        tvAddress.setText("📍 " + m.address);
        tvFeeder.setText("ফিডার: " + m.feeder);
        tvPhase.setText("ফেজ: " + m.phase);
        tvBalance.setText("৳ " + m.balance);
        tvUsage.setText("৳ " + String.format("%.2f", m.monthlyUsage));
        tvReading.setText("সর্বশেষ: " + m.lastReading);

        // Strategy pattern — estimated bill
        double estBill = new ResidentialBillingStrategy().calculate(m.monthlyUsage);
        tvEstBill.setText("৳ " + String.format("%.2f", m.monthlyUsage));

        // State pattern
        MeterState state = MeterStateFactory.resolve(m.balance, activeMeter.lowBalanceThreshold);
        tvStateLabel.setText(state.getStateLabel());
        tvStateLabel.setTextColor(state.getStateColor());
        cardBalanceState.setCardBackgroundColor(
                state.showAlert() ? Color.parseColor("#FFF3E0") : Color.WHITE);

        if (state.showAlert()) {
            cardAlert.setVisibility(View.VISIBLE);
            tvAlertBanner.setText("⚠️ ব্যালেন্স কম! দ্রুত রিচার্জ করুন। নির্ধারিত সীমা: ৳ "
                    + activeMeter.lowBalanceThreshold);
        }
    }

    private void loadMonthlyChart() {
        Calendar cal = Calendar.getInstance();
        String to = cal.get(Calendar.YEAR) + String.format("%02d", cal.get(Calendar.MONTH) + 1);
        cal.add(Calendar.MONTH, -5);
        String from = cal.get(Calendar.YEAR) + String.format("%02d", cal.get(Calendar.MONTH) + 1);

        DescoServiceFacade.getInstance().fetchMonthly(activeMeter.accountNo, from, to,
                new Callback<MonthlyResponse>() {
                    @Override
                    public void onResponse(Call<MonthlyResponse> call, Response<MonthlyResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null
                                && response.body().data != null) {
                            requireActivity().runOnUiThread(() ->
                                    renderChart(response.body().data));
                        }
                    }
                    @Override public void onFailure(Call<MonthlyResponse> call, Throwable t) {}
                });
    }

    private void renderChart(List<MonthlyResponse.MonthlyRecord> records) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            entries.add(new BarEntry(i, (float) records.get(i).consumption));
            labels.add(records.get(i).month);
        }

        BarDataSet dataSet = new BarDataSet(entries, "kWh ব্যবহার");
        dataSet.setColor(Color.parseColor("#1565C0"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(600);
        barChart.invalidate();
    }
}