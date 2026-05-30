package com.example.descosmartapp.ui.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.Fragment;

import com.example.descosmartapp.R;
import com.example.descosmartapp.db.AppDatabase;
import com.example.descosmartapp.db.MeterProfile;
import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.model.MonthlyResponse;
import com.example.descosmartapp.model.RechargeResponse;
import com.example.descosmartapp.pattern.builder.PdfReportBuilder;
import com.example.descosmartapp.pattern.facade.DescoServiceFacade;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsFragment extends Fragment {

    Button btnGenerate;
    ProgressBar progress;
    TextView tvStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reports, container, false);
        btnGenerate = v.findViewById(R.id.btnGenerate);
        progress    = v.findViewById(R.id.progressReport);
        tvStatus    = v.findViewById(R.id.tvReportStatus);

        btnGenerate.setOnClickListener(x -> generateReport());
        return v;
    }

    private void generateReport() {
        MeterProfile meter = AppDatabase.getInstance(requireContext()).meterDao().getActiveMeter();
        if (meter == null) return;

        progress.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(false);
        tvStatus.setText("ডেটা সংগ্রহ করা হচ্ছে...");

        AtomicInteger done = new AtomicInteger(0);
        AtomicReference<BalanceResponse.Balance> balRef = new AtomicReference<>();
        AtomicReference<List<MonthlyResponse.MonthlyRecord>> monthRef = new AtomicReference<>();
        AtomicReference<List<RechargeResponse.RechargeRecord>> rechRef = new AtomicReference<>();

        Runnable checkAndBuild = () -> {
            if (done.incrementAndGet() >= 3) {
                requireActivity().runOnUiThread(() ->
                        buildPdf(meter, balRef.get(), monthRef.get(), rechRef.get()));
            }
        };

        // Balance
        DescoServiceFacade.getInstance().fetchBalance(meter.accountNo, new Callback<BalanceResponse>() {
            @Override public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> r) {
                if (r.isSuccessful() && r.body() != null) balRef.set(r.body().data);
                checkAndBuild.run();
            }
            @Override public void onFailure(Call<BalanceResponse> call, Throwable t) { checkAndBuild.run(); }
        });

        // Monthly — YYYYMM format, last 12 months
        Calendar cal = Calendar.getInstance();
        String toMonth = String.format("%04d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
        cal.add(Calendar.MONTH, -11);
        String fromMonth = String.format("%04d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

        DescoServiceFacade.getInstance().fetchMonthly(meter.accountNo, fromMonth, toMonth,
                new Callback<MonthlyResponse>() {
                    @Override public void onResponse(Call<MonthlyResponse> call, Response<MonthlyResponse> r) {
                        if (r.isSuccessful() && r.body() != null) monthRef.set(r.body().data);
                        checkAndBuild.run();
                    }
                    @Override public void onFailure(Call<MonthlyResponse> call, Throwable t) { checkAndBuild.run(); }
                });

        // Recharge — yyyy-MM-dd format, last 1 year
        Calendar c2 = Calendar.getInstance();
        String toDate = String.format("%04d-%02d-%02d",
                c2.get(Calendar.YEAR), c2.get(Calendar.MONTH) + 1, c2.get(Calendar.DAY_OF_MONTH));
        c2.add(Calendar.YEAR, -1);
        String fromDate = String.format("%04d-%02d-%02d",
                c2.get(Calendar.YEAR), c2.get(Calendar.MONTH) + 1, c2.get(Calendar.DAY_OF_MONTH));

        DescoServiceFacade.getInstance().fetchRechargeHistory(meter.accountNo, fromDate, toDate,
                new Callback<RechargeResponse>() {
                    @Override public void onResponse(Call<RechargeResponse> call, Response<RechargeResponse> r) {
                        if (r.isSuccessful() && r.body() != null) rechRef.set(r.body().data);
                        checkAndBuild.run();
                    }
                    @Override public void onFailure(Call<RechargeResponse> call, Throwable t) { checkAndBuild.run(); }
                });
    }

    private void buildPdf(MeterProfile meter, BalanceResponse.Balance bal,
                          List<MonthlyResponse.MonthlyRecord> monthly,
                          List<RechargeResponse.RechargeRecord> recharge) {
        tvStatus.setText("রিপোর্ট তৈরি হচ্ছে...");

        String path = PdfReportBuilder.newBuilder(requireContext())
                .setMeter(meter)
                .setBalance(bal)
                .setMonthlyData(monthly)
                .setRechargeData(recharge)
                .setTitle("DESCO বিদ্যুৎ রিপোর্ট — " + meter.label)
                .build();

        progress.setVisibility(View.GONE);
        btnGenerate.setEnabled(true);

        if (path != null) {
            tvStatus.setText("✅ রিপোর্ট সেভ হয়েছে:\n" + path);
            Toast.makeText(requireContext(), "Download folder এ সেভ হয়েছে!", Toast.LENGTH_LONG).show();
        } else {
            tvStatus.setText("❌ রিপোর্ট তৈরি ব্যর্থ হয়েছে।");
        }
    }
}