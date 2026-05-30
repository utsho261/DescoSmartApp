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

    private final AtomicBoolean isLoading = new AtomicBoolean(false);
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
        tvMonthlyBDT    = v.findViewById(R.id.tvUsage);
        tvReading       = v.findViewById(R.id.tvReading);
        tvStateLabel    = v.findViewById(R.id.tvStateLabel);
        tvEstBill       = v.findViewById(R.id.tvEstBill);
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

    private void loadAll() {
        if (!isLoading.compareAndSet(false, true)) return;

        apiDoneCount.set(0);
        customerRef.set(null);
        balanceRef.set(null);

        progressDash.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);
        cardAlert.setVisibility(View.GONE);
        cardSuggest.setVisibility(View.GONE);
        tvChartEmpty.setVisibility(View.GONE);

        final String accountNo = activeMeter.accountNo;

        DescoServiceFacade.getInstance().fetchCustomerByAccount(accountNo, new Callback<CustomerResponse>() {
            @Override
            public void onResponse(Call<CustomerResponse> call, Response<CustomerResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null)
                    customerRef.set(response.body().data);
                onOneCallDone();
            }
            @Override public void onFailure(Call<CustomerResponse> call, Throwable t) { onOneCallDone(); }
        });

        DescoServiceFacade.getInstance().fetchBalance(accountNo, new Callback<BalanceResponse>() {
            @Override
            public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    BalanceResponse.Balance b = response.body().data;
                    if (b.balance > 0 || b.currentMonthConsumption > 0 || b.readingTime != null)
                        balanceRef.set(b);
                }
                onOneCallDone();
            }
            @Override public void onFailure(Call<BalanceResponse> call, Throwable t) { onOneCallDone(); }
        });

        loadMonthlyChart();
    }

    private void onOneCallDone() {
        int done = apiDoneCount.incrementAndGet();
        if (done < 2) return;
        if (!isAdded()) { isLoading.set(false); return; }

        new Handler(Looper.getMainLooper()).post(() -> {
            isLoading.set(false);
            progressDash.setVisibility(View.GONE);
            btnRefresh.setEnabled(true);
            CustomerApiAdapter.UnifiedCustomerModel model =
                    CustomerApiAdapter.adapt(customerRef.get(), balanceRef.get());
            updateUI(model, balanceRef.get() != null);
        });
    }

    /**
     * ══════════════════════════════════════════════════════════════════
     * আনুমানিক বিল — দিন-ভিত্তিক average পদ্ধতি
     *
     * যেমন: আজ ৩০ মে, এই মাসে ৮৫৭.৯৭ টাকা খরচ হয়েছে
     *   → daily avg = 857.97 ÷ 30 = 28.60 tk/day
     *   → মাসে ৩১ দিন → আনুমানিক মাসিক = 28.60 × 31 = ৮৮৬.৫৩ টাকা
     * ══════════════════════════════════════════════════════════════════
     */
    private double calculateEstimatedMonthlyBill(double usedBDTSoFar) {
        Calendar cal = Calendar.getInstance();
        int todayDate = cal.get(Calendar.DAY_OF_MONTH);
        int totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (todayDate <= 0) todayDate = 1;
        double dailyAverage = usedBDTSoFar / todayDate;
        return Math.round(dailyAverage * totalDays * 100.0) / 100.0;
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
            double balance          = m.balance;            // current balance (টাকা)
            double usedThisMonth   = m.monthlyBDT;         // এই মাসে ইতিমধ্যে খরচ (BDT)
            double estimatedFull   = calculateEstimatedMonthlyBill(usedThisMonth); // পুরো মাসের আনুমানিক

            tvBalance.setText("৳ " + String.format("%.2f", balance));
            tvMonthlyBDT.setText(String.format("৳ %.2f", usedThisMonth));
            tvReading.setText("সর্বশেষ: " + m.lastReading);
            tvEstBill.setText(String.format("আনুমানিক বিল: ৳ %.2f", estimatedFull));

            // ══════════════════════════════════════════════════════════
            // সঠিক calculation:
            //
            //   বাকি মাসের সম্ভাব্য খরচ = আনুমানিক পুরো মাসের বিল − এই মাসে ইতিমধ্যে খরচ
            //   দরকারি রিচার্জ          = বাকি সম্ভাব্য খরচ − current balance
            //
            //   যদি (বাকি সম্ভাব্য খরচ − current balance) > 0
            //     → এই পরিমাণ রিচার্জ করতে হবে
            //   যদি ≤ 0
            //     → ব্যালেন্স যথেষ্ট আছে
            //
            // উদাহরণ (screenshot থেকে):
            //   usedThisMonth   = ৮৫৭.৯৭ (API: currentMonthConsumption)
            //   estimatedFull   ≈ ৮৮৭.৫৩ (30 দিনে ৮৫৭.৯৭, মাসে ৩১ দিন)
            //   remainingCost   = ৮৮৭.৫৩ − ৮৫৭.৯৭ = ২৯.৫৬ (বাকি ১ দিনের খরচ)
            //   balance         = ৪৭২.৭৩
            //   needToRecharge  = ২৯.৫৬ − ৪৭২.৭৩ = −৪৪৩.১৭ (negative → যথেষ্ট আছে)
            // ══════════════════════════════════════════════════════════
            double remainingEstimatedCost = estimatedFull - usedThisMonth;  // বাকি মাসের সম্ভাব্য খরচ
            double needToRecharge         = remainingEstimatedCost - balance; // কত রিচার্জ দরকার

            cardSuggest.setVisibility(View.VISIBLE);

            if (needToRecharge > 0) {
                // Negative balance situation — রিচার্জ দরকার
                double suggested = Math.ceil(needToRecharge / 100.0) * 100.0; // ১০০ এর গুণিতকে round up
                tvSuggestRecharge.setText(
                        "💡 এই মাসে বিল ৳" + String.format("%.0f", estimatedFull) +
                                " কিন্তু ব্যালেন্স মাত্র ৳" + String.format("%.2f", balance) +
                                "।\nআরও প্রায় ৳" + String.format("%.0f", suggested) + " রিচার্জ করুন।"
                );
                // State: low balance চেক
                MeterState state = MeterStateFactory.resolve(balance, activeMeter.lowBalanceThreshold);
                tvStateLabel.setText(state.getStateLabel());
                tvStateLabel.setTextColor(state.getStateColor());
                if (state.showAlert()) {
                    cardAlert.setVisibility(View.VISIBLE);
                    tvAlertBanner.setText("⚠️ ব্যালেন্স কম! দ্রুত রিচার্জ করুন। নির্ধারিত সীমা: ৳ "
                            + activeMeter.lowBalanceThreshold);
                }
            } else {
                // ব্যালেন্স যথেষ্ট
                tvSuggestRecharge.setText(
                        "✅ এই মাসে বিল ৳" + String.format("%.0f", estimatedFull) +
                                " — ব্যালেন্স ৳" + String.format("%.2f", balance) + " যথেষ্ট আছে।"
                );
                MeterState state = MeterStateFactory.resolve(balance, activeMeter.lowBalanceThreshold);
                tvStateLabel.setText(state.getStateLabel());
                tvStateLabel.setTextColor(state.getStateColor());
                if (state.showAlert()) {
                    cardAlert.setVisibility(View.VISIBLE);
                    tvAlertBanner.setText("⚠️ ব্যালেন্স কম! দ্রুত রিচার্জ করুন। নির্ধারিত সীমা: ৳ "
                            + activeMeter.lowBalanceThreshold);
                }
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

    private void loadMonthlyChart() {
        Calendar cal = Calendar.getInstance();
        String to = String.format("%04d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
        cal.add(Calendar.MONTH, -11);
        String from = String.format("%04d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

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
            entries.add(new BarEntry(i, (float) r.consumption));

            String raw = r.month != null ? r.month : "";
            String label;
            if (raw.length() >= 6) {
                try {
                    int year  = Integer.parseInt(raw.substring(0, 4));
                    int month = Integer.parseInt(raw.substring(4, 6));
                    String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                            "Jul","Aug","Sep","Oct","Nov","Dec"};
                    label = months[Math.min(month - 1, 11)] + "\n" + String.valueOf(year).substring(2);
                } catch (NumberFormatException e) {
                    label = raw;
                }
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
        xAxis.setLabelRotationAngle(-30f);
        xAxis.setTextColor(Color.parseColor("#555555"));
        xAxis.setLabelCount(records.size(), true);

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setTextColor(Color.parseColor("#555555"));
        barChart.getAxisLeft().setGridColor(Color.parseColor("#EEEEEE"));
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setExtraBottomOffset(20f);
        barChart.animateY(700);
        barChart.invalidate();
    }
}