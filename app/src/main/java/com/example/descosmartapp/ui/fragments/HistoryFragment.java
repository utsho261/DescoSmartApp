package com.example.descosmartapp.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    RecyclerView recyclerView;
    ProgressBar  progress;
    TextView     tvEmpty, tvMonthTotal, tvYearTotal;

    private MeterProfile activeMeter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = v.findViewById(R.id.recyclerHistory);
        progress     = v.findViewById(R.id.progressHistory);
        tvEmpty      = v.findViewById(R.id.tvEmpty);
        tvMonthTotal = v.findViewById(R.id.tvMonthTotal);
        tvYearTotal  = v.findViewById(R.id.tvYearTotal);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        activeMeter = AppDatabase.getInstance(requireContext()).meterDao().getActiveMeter();
        if (activeMeter != null) {
            fetchSplitAndMerge(activeMeter.accountNo);
        } else {
            showEmpty("কোনো মিটার যোগ করা নেই");
        }

        return v;
    }

    /**
     * ══════════════════════════════════════════════════════════════════
     * SPLIT + MERGE STRATEGY
     *
     * API সর্বোচ্চ ৬ মাস দেয়। তাই ২টি call করে merge করা হয়:
     *
     *   Part 1: আজ থেকে ১২ মাস আগে → ৬ মাস আগে
     *   Part 2: ৬ মাস আগে + ১ দিন  → আজ
     *
     * উদাহরণ (আজ ৩০ মে ২০২৬):
     *   Part 1: 2025-05-30 → 2025-11-30
     *   Part 2: 2025-12-01 → 2026-05-30
     *
     * দুটো response এলে একসাথে merge করে sort করে দেখানো হয়।
     * ══════════════════════════════════════════════════════════════════
     */
    private void fetchSplitAndMerge(String accountNo) {
        showLoading();

        // আজকের তারিখ
        Calendar today = Calendar.getInstance();
        String dateTo2 = formatDate(today);                     // Part 2 এর শেষ = আজ

        // ৬ মাস আগে = Part 2 এর শুরু - 1 দিন = Part 1 এর শেষ
        Calendar sixMonthsAgo = Calendar.getInstance();
        sixMonthsAgo.add(Calendar.MONTH, -6);

        Calendar part1End = (Calendar) sixMonthsAgo.clone();   // Part 1 শেষ
        String dateTo1 = formatDate(part1End);                 // e.g. "2025-11-30"

        Calendar part2Start = (Calendar) sixMonthsAgo.clone();
        part2Start.add(Calendar.DAY_OF_MONTH, 1);              // Part 2 শুরু = Part 1 শেষ + 1 দিন
        String dateFrom2 = formatDate(part2Start);             // e.g. "2025-12-01"

        // ১২ মাস আগে = Part 1 এর শুরু
        Calendar twelveMonthsAgo = Calendar.getInstance();
        twelveMonthsAgo.add(Calendar.MONTH, -12);
        String dateFrom1 = formatDate(twelveMonthsAgo);        // e.g. "2025-05-30"

        Log.d(TAG, "Part 1: " + dateFrom1 + " → " + dateTo1);
        Log.d(TAG, "Part 2: " + dateFrom2 + " → " + dateTo2);

        // দুটো call এর result store করার জন্য
        AtomicInteger doneCount = new AtomicInteger(0);
        AtomicReference<List<RechargeResponse.RechargeRecord>> part1Ref = new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<RechargeResponse.RechargeRecord>> part2Ref = new AtomicReference<>(new ArrayList<>());

        Runnable onBothDone = () -> {
            if (doneCount.incrementAndGet() < 2) return; // দুটো শেষ হলে merge
            if (!isAdded()) return;

            // Merge: Part 1 + Part 2
            List<RechargeResponse.RechargeRecord> merged = new ArrayList<>();
            merged.addAll(part1Ref.get());
            merged.addAll(part2Ref.get());

            Log.d(TAG, "Merged total: " + merged.size() + " records"
                    + " (Part1=" + part1Ref.get().size() + ", Part2=" + part2Ref.get().size() + ")");

            new Handler(Looper.getMainLooper()).post(() -> {
                if (merged.isEmpty()) {
                    showEmpty("কোনো রিচার্জ ইতিহাস পাওয়া যায়নি");
                } else {
                    showData(merged);
                }
            });
        };

        // ── Part 1 call ──
        DescoServiceFacade.getInstance().fetchRechargeHistory(
                accountNo, dateFrom1, dateTo1,
                new Callback<RechargeResponse>() {
                    @Override
                    public void onResponse(Call<RechargeResponse> call, Response<RechargeResponse> response) {
                        List<RechargeResponse.RechargeRecord> list = extractList(response, "Part1");
                        if (list != null) part1Ref.set(list);
                        onBothDone.run();
                    }
                    @Override
                    public void onFailure(Call<RechargeResponse> call, Throwable t) {
                        Log.e(TAG, "Part1 failed: " + t.getMessage());
                        onBothDone.run();
                    }
                });

        // ── Part 2 call ──
        DescoServiceFacade.getInstance().fetchRechargeHistory(
                accountNo, dateFrom2, dateTo2,
                new Callback<RechargeResponse>() {
                    @Override
                    public void onResponse(Call<RechargeResponse> call, Response<RechargeResponse> response) {
                        List<RechargeResponse.RechargeRecord> list = extractList(response, "Part2");
                        if (list != null) part2Ref.set(list);
                        onBothDone.run();
                    }
                    @Override
                    public void onFailure(Call<RechargeResponse> call, Throwable t) {
                        Log.e(TAG, "Part2 failed: " + t.getMessage());
                        onBothDone.run();
                    }
                });
    }

    private List<RechargeResponse.RechargeRecord> extractList(Response<RechargeResponse> response, String tag) {
        if (!response.isSuccessful()) {
            Log.e(TAG, tag + " HTTP error: " + response.code());
            return null;
        }
        if (response.body() == null) {
            Log.e(TAG, tag + " body null");
            return null;
        }
        List<RechargeResponse.RechargeRecord> list = response.body().data;
        Log.d(TAG, tag + " records: " + (list != null ? list.size() : 0));
        return list;
    }

    // ══════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════
    private void showLoading() {
        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvMonthTotal.setText("হিসাব হচ্ছে...");
        tvYearTotal.setText("হিসাব হচ্ছে...");
    }

    private void showData(List<RechargeResponse.RechargeRecord> list) {
        progress.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        computeAndShowTotals(list);
        recyclerView.setAdapter(new RechargeAdapter(list));
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmpty(String message) {
        progress.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        tvMonthTotal.setText("৳ 0");
        tvYearTotal.setText("৳ 0");
    }

    // ══════════════════════════════════════════════════════
    // এই মাসে ও এই বছরে মোট রিচার্জ
    // ══════════════════════════════════════════════════════
    private void computeAndShowTotals(List<RechargeResponse.RechargeRecord> records) {
        Calendar now  = Calendar.getInstance();
        int thisYear  = now.get(Calendar.YEAR);
        int thisMonth = now.get(Calendar.MONTH) + 1;

        double monthTotal = 0, yearTotal = 0;

        for (RechargeResponse.RechargeRecord r : records) {
            if (r.rechargeDate == null || r.rechargeDate.length() < 7) continue;
            try {
                String dateOnly = r.rechargeDate.replace("T", " ")
                        .substring(0, Math.min(10, r.rechargeDate.length())).trim();
                String[] parts = dateOnly.split("-");
                if (parts.length < 2) continue;
                int year  = Integer.parseInt(parts[0].trim());
                int month = Integer.parseInt(parts[1].trim());
                if (year == thisYear) {
                    yearTotal += r.amount;
                    if (month == thisMonth) monthTotal += r.amount;
                }
            } catch (Exception e) {
                Log.w(TAG, "Date parse error: " + r.rechargeDate);
            }
        }

        tvMonthTotal.setText(String.format("৳ %.0f", monthTotal));
        tvYearTotal.setText(String.format("৳ %.0f", yearTotal));
    }

    private String formatDate(Calendar cal) {
        return String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
}