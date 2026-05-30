package com.example.descosmartapp.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    RecyclerView recyclerView;
    ProgressBar  progress;
    TextView     tvEmpty, tvMonthTotal, tvYearTotal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = v.findViewById(R.id.recyclerHistory);
        progress     = v.findViewById(R.id.progressHistory);
        tvEmpty      = v.findViewById(R.id.tvEmpty);
        tvMonthTotal = v.findViewById(R.id.tvMonthTotal);
        tvYearTotal  = v.findViewById(R.id.tvYearTotal);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        MeterProfile meter = AppDatabase.getInstance(requireContext()).meterDao().getActiveMeter();
        if (meter != null) loadHistory(meter);

        return v;
    }

    private void loadHistory(MeterProfile meter) {
        progress.setVisibility(View.VISIBLE);
        tvMonthTotal.setText("হিসাব হচ্ছে...");
        tvYearTotal.setText("হিসাব হচ্ছে...");

        // Fetch last 13 months to safely cover full current year + last year
        Calendar cal = Calendar.getInstance();
        String to = formatDate(cal);          // today  e.g. "2026-05-30"
        cal.add(Calendar.MONTH, -13);
        String from = formatDate(cal);        // 13 months ago

        DescoServiceFacade.getInstance().fetchRechargeHistory(
                meter.accountNo, from, to,
                new Callback<RechargeResponse>() {
                    @Override
                    public void onResponse(Call<RechargeResponse> call, Response<RechargeResponse> response) {
                        if (!isAdded()) return;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            progress.setVisibility(View.GONE);

                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().data != null
                                    && !response.body().data.isEmpty()) {

                                List<RechargeResponse.RechargeRecord> all = response.body().data;
                                computeAndShowTotals(all);
                                recyclerView.setAdapter(new RechargeAdapter(all));
                                tvEmpty.setVisibility(View.GONE);
                            } else {
                                showEmpty();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<RechargeResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            progress.setVisibility(View.GONE);
                            tvEmpty.setText("লোড হয়নি। পুনরায় চেষ্টা করুন।");
                            showEmpty();
                        });
                    }
                });
    }

    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        tvMonthTotal.setText("৳ 0");
        tvYearTotal.setText("৳ 0");
    }

    /**
     * Parses rechargeDate strings like:
     *   "2026-05-24 08:35:51"  or  "2026-05-24T08:35:51"  or  "2026-05-24T08:35:51.000Z"
     * Sums amounts for this month and this calendar year.
     */
    private void computeAndShowTotals(List<RechargeResponse.RechargeRecord> records) {
        Calendar now      = Calendar.getInstance();
        int thisYear  = now.get(Calendar.YEAR);
        int thisMonth = now.get(Calendar.MONTH) + 1; // 1-based

        double monthTotal = 0;
        double yearTotal  = 0;

        for (RechargeResponse.RechargeRecord r : records) {
            if (r.rechargeDate == null || r.rechargeDate.length() < 7) continue;
            try {
                // Normalize: replace T with space, take the date part before space or dot
                String dateOnly = r.rechargeDate
                        .replace("T", " ")
                        .split("[\\. ]")[0];      // "2026-05-24"
                String[] parts = dateOnly.split("-");
                if (parts.length < 3) continue;

                int year  = Integer.parseInt(parts[0].trim());
                int month = Integer.parseInt(parts[1].trim());

                if (year == thisYear) {
                    yearTotal += r.amount;
                    if (month == thisMonth) {
                        monthTotal += r.amount;
                    }
                }
            } catch (Exception ignored) { /* skip malformed */ }
        }

        tvMonthTotal.setText(String.format("৳ %.0f", monthTotal));
        tvYearTotal.setText(String.format("৳ %.0f", yearTotal));
    }

    /** "yyyy-MM-dd" */
    private String formatDate(Calendar cal) {
        return String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
}