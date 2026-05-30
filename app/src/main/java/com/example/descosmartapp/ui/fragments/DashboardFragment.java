package com.example.descosmartapp.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    TextView tvName, tvAccount, tvMeter, tvPhone, tvAddress, tvFeeder, tvPhase;
    TextView tvBalance, tvMonthlyBDT, tvReading, tvStateLabel, tvEstBill;
    TextView tvAlertBanner, tvSuggestRecharge, tvChartEmpty;
    CardView cardBalanceState, cardAlert, cardSuggest;
    ProgressBar progressDash;
    Button btnRefresh;
    BarChart barChart;

    AppDatabase db;
    MeterProfile activeMeter;

    // Prevent concurrent loads
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    // Hold both results until both API calls complete
    private final AtomicReference<CustomerResponse.Customer> customerRef = new AtomicReference<>();
    private final AtomicReference<BalanceResponse.Balance>   balanceRef  = new AtomicReference<>();
    private final AtomicInteger apiDoneCount = new AtomicInteger(0);

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
        tvName          = v.findViewById(R.id.tvName);
        tvAccount       = v.findViewById(R.id.tvAccount);
        tvMeter         = v.findViewById(R.id.tvMeter);
        tvPhone         = v.findViewById(R.id.tvPhone);
        tvAddress       = v.findViewById(R.id.tvAddress);
        tvFeeder        = v.findViewById(R.id.tvFeeder);
        tvPhase         = v.findViewById(R.id.tvPhase);
        tvBalance       = v.findViewById(R.id.tvBalance);
        tvMonthlyBDT    = v.findViewById(R.id.tvUsage);      // "এই মাসের বিল (BDT)"
        tvReading       = v.findViewById(R.id.tvReading);
        tvStateLabel    = v.findViewById(R.id.tvStateLabel);
        tvEstBill       = v.findViewById(R.id.tvEstBill);    // suggested recharge label
        tvAlertBanner   = v.findViewById(R.id.tvAlertBanner);
        tvSuggestRecharge = v.findViewById(R.id.tvSuggestRecharge);
        tvChartEmpty    = v.findViewById(R.id.tvChartEmpty);
        cardBalanceState = v.findViewById(R.id.cardBalanceState);
        cardAlert       = v.findViewById(R.id.cardAlert);
        cardSuggest     = v.findViewById(R.id.cardSuggest);
        progressDash    = v.findViewById(R.id.progressDash);
        btnRefresh      = v.findViewById(R.id.btnRefresh);
        barChart        = v.findViewById(R.id.barChart);
    }

    /**
     * Loads customer info and balance in parallel.
     * Only renders UI when BOTH calls complete (or fail).
     * AtomicBoolean prevents double-loading.
     */
    private void loadAll() {
        if (!isLoading.compareAndSet(false, true)) return;

        // Reset for this cycle
        apiDoneCount.set(0);
        customerRef.set(null);
        balanceRef.set(null);

        progressDash.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);
        cardAlert.setVisibility(View.GONE);
        cardSuggest.setVisibility(View.GONE);
        tvChartEmpty.setVisibility(View.GONE);

        final String accountNo = activeMeter.accountNo;

        // Call 1: Customer info
        DescoServiceFacade.getInstance().fetchCustomerByAccount(accountNo, new Callback<CustomerResponse>() {
            @Override
            public void onResponse(Call<CustomerResponse> call, Response<CustomerResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    customerRef.set(response.body().data);
                }
                onOneCallDone();
            }
            @Override public void onFailure(Call<CustomerResponse> call, Throwable t) { onOneCallDone(); }
        });

        // Call 2: Balance (has currentMonthConsumption = BDT)
        DescoServiceFacade.getInstance().fetchBalance(accountNo, new Callback<BalanceResponse>() {
            @Override
            public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    BalanceResponse.Balance b = response.body().data;
                    // Guard against totally-empty response (all zeros, no timestamp)
                    if (b.balance > 0 || b.currentMonthConsumption > 0 || b.readingTime != null) {
                        balanceRef.set(b);
                    }
                }
                onOneCallDone();
            }
            @Override public void onFailure(Call<BalanceResponse> call, Throwable t) { onOneCallDone(); }
        });

        // Chart loads independently (does not block main data)
        loadMonthlyChart();
    }

    /** Renders UI once both API calls finish */
    private void onOneCallDone() {
        int done = apiDoneCount.incrementAndGet();
        if (done < 2) return;

        if (!isAdded()) { isLoading.set(false); return; }

        new Handler(Looper.getMainLooper()).post(() -> {
            isLoading.set(false);
            progressDash.setVisibility(View.GONE);
            btnRefresh.setEnabled(true);

            CustomerResponse.Customer c = customerRef.get();
            BalanceResponse.Balance   b = balanceRef.get();

            CustomerApiAdapter.UnifiedCustomerModel model = CustomerApiAdapter.adapt(c, b);
            boolean hasBalance = (b != null);
            updateUI(model, hasBalance);
        });
    }

    private void updateUI(CustomerApiAdapter.UnifiedCustomerModel m, boolean hasBalance) {
        tvName.setText(m.displayName);
        tvAccount.setText("Account: " + m.accountNo);
        tvMeter.setText("Meter: " + m.meterNo);
        tvPhone.setText(m.phone);
        tvAddress.setText(m.address);
        tvFeeder.setText("ফিডার: " + m.feeder);
        tvPhase.setText("ফেজ: " + m.phase);

        if (hasBalance) {
            // Balance
            tvBalance.setText("৳ " + String.format("%.2f", m.balance));

            // currentMonthConsumption = BDT from API (website confirms: "In BDT: 857.97 BDT")
            tvMonthlyBDT.setText(String.format("৳ %.2f", m.monthlyBDT));

            // Reading time
            tvReading.setText("সর্বশেষ: " + m.lastReading);

            // Suggested recharge: how much more needed to cover the monthly bill
            // If monthlyBDT > balance → need to top up
            double gap = m.monthlyBDT - m.balance;
            if (gap > 0) {
                // Round up to nearest 100
                double suggested = Math.ceil(gap / 100.0) * 100.0;
                tvEstBill.setText("আনুমানিক বিল: ৳ " + String.format("%.2f", m.monthlyBDT));
                cardSuggest.setVisibility(View.VISIBLE);
                tvSuggestRecharge.setText(
                        "💡 এই মাসের বিল ৳" + String.format("%.0f", m.monthlyBDT) +
                                " কিন্তু ব্যালেন্স মাত্র ৳" + String.format("%.2f", m.balance) +
                                "।\nআরও প্রায় ৳" + String.format("%.0f", suggested) + " রিচার্জ করুন।"
                );
            } else {
                tvEstBill.setText("আনুমানিক বিল: ৳ " + String.format("%.2f", m.monthlyBDT));
                cardSuggest.setVisibility(View.VISIBLE);
                tvSuggestRecharge.setText(
                        "✅ এই মাসের বিল ৳" + String.format("%.0f", m.monthlyBDT) +
                                " — ব্যালেন্স ৳" + String.format("%.2f", m.balance) + " যথেষ্ট আছে।"
                );
            }

            // State pattern
            MeterState state = MeterStateFactory.resolve(m.balance, activeMeter.lowBalanceThreshold);
            tvStateLabel.setText(state.getStateLabel());
            tvStateLabel.setTextColor(state.getStateColor());

            if (state.showAlert()) {
                cardAlert.setVisibility(View.VISIBLE);
                tvAlertBanner.setText("⚠️ ব্যালেন্স কম! দ্রুত রিচার্জ করুন। নির্ধারিত সীমা: ৳ "
                        + activeMeter.lowBalanceThreshold);
            }
        } else {
            tvBalance.setText("৳ —");
            tvMonthlyBDT.setText("৳ —");
            tvReading.setText("সর্বশেষ: —");
            tvEstBill.setText("আনুমানিক বিল: —");
            tvStateLabel.setText("সক্রিয়");
            tvStateLabel.setTextColor(Color.parseColor("#2E7D32"));
        }
    }

    /**
     * Monthly chart — uses YYYYMM format for the API.
     * Website shows last 12 months data, so we fetch 6 months here.
     */
    private void loadMonthlyChart() {
        Calendar cal = Calendar.getInstance();
        // "to" = this month e.g. "202605"
        String to = String.format("%04d%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1);

        // "from" = 5 months ago
        cal.add(Calendar.MONTH, -5);
        String from = String.format("%04d%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1);

        DescoServiceFacade.getInstance().fetchMonthly(
                activeMeter.accountNo, from, to,
                new Callback<MonthlyResponse>() {
                    @Override
                    public void onResponse(Call<MonthlyResponse> call, Response<MonthlyResponse> response) {
                        if (!isAdded()) return;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().data != null
                                    && !response.body().data.isEmpty()) {
                                tvChartEmpty.setVisibility(View.GONE);
                                renderChart(response.body().data);
                            } else {
                                showChartEmpty();
                            }
                        });
                    }
                    @Override
                    public void onFailure(Call<MonthlyResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        new Handler(Looper.getMainLooper()).post(DashboardFragment.this::showChartEmpty);
                    }
                });
    }

    private void showChartEmpty() {
        tvChartEmpty.setVisibility(View.VISIBLE);
        barChart.clear();
        barChart.setNoDataText("");
        barChart.invalidate();
    }

    private void renderChart(List<MonthlyResponse.MonthlyRecord> records) {
        List<BarEntry> entries = new ArrayList<>();
        List<String>   labels  = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            MonthlyResponse.MonthlyRecord r = records.get(i);
            // consumption = kWh units
            entries.add(new BarEntry(i, (float) r.consumption));

            // Month label: "202504" → "Apr\n25"
            String raw = r.month != null ? r.month : "";
            String label;
            if (raw.length() >= 6) {
                int year  = Integer.parseInt(raw.substring(0, 4));
                int month = Integer.parseInt(raw.substring(4, 6));
                String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                        "Jul","Aug","Sep","Oct","Nov","Dec"};
                label = months[Math.min(month - 1, 11)] + "\n" + String.valueOf(year).substring(2);
            } else {
                label = raw;
            }
            labels.add(label);
        }

        BarDataSet ds = new BarDataSet(entries, "kWh");
        ds.setColor(Color.parseColor("#1565C0"));
        ds.setValueTextColor(Color.parseColor("#1565C0"));
        ds.setValueTextSize(9f);

        BarData data = new BarData(ds);
        data.setBarWidth(0.55f);
        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(9f);
        xAxis.setTextColor(Color.parseColor("#555555"));
        xAxis.setLabelCount(records.size());

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setTextColor(Color.parseColor("#555555"));
        barChart.getAxisLeft().setGridColor(Color.parseColor("#EEEEEE"));
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setExtraBottomOffset(10f);
        barChart.animateY(700);
        barChart.invalidate();
    }
}