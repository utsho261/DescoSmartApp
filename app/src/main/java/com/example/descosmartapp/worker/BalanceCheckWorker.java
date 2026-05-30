package com.example.descosmartapp.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.descosmartapp.db.AppDatabase;
import com.example.descosmartapp.db.MeterProfile;
import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.pattern.decorator.BasicNotificationSender;
import com.example.descosmartapp.pattern.decorator.UrgentNotificationDecorator;
import com.example.descosmartapp.pattern.facade.DescoServiceFacade;
import com.example.descosmartapp.pattern.state.MeterState;
import com.example.descosmartapp.pattern.state.MeterStateFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Runs in background via WorkManager.
 * Checks balance for ALL saved meters and fires notifications if needed.
 */
public class BalanceCheckWorker extends Worker {

    private static final String TAG = "BalanceCheckWorker";

    public BalanceCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<MeterProfile> meters = AppDatabase
                    .getInstance(getApplicationContext())
                    .meterDao()
                    .getAllMeters();

            for (MeterProfile meter : meters) {
                checkMeter(meter);
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Worker failed", e);
            return Result.retry();
        }
    }

    private void checkMeter(MeterProfile meter) {
        if (meter.accountNo == null || meter.accountNo.isEmpty()) return;

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Double> balanceRef = new AtomicReference<>(-1.0);

        DescoServiceFacade.getInstance().fetchBalance(meter.accountNo, new Callback<BalanceResponse>() {
            @Override
            public void onResponse(Call<BalanceResponse> call, Response<BalanceResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {
                    balanceRef.set(response.body().data.balance);
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Call<BalanceResponse> call, Throwable t) {
                latch.countDown();
            }
        });

        try { latch.await(10, java.util.concurrent.TimeUnit.SECONDS); }
        catch (InterruptedException ignored) {}

        double balance = balanceRef.get();
        if (balance < 0) return; // fetch failed

        MeterState state = MeterStateFactory.resolve(balance, meter.lowBalanceThreshold);

        if (state.showAlert()) {
            String title = "⚠️ " + meter.label + " — ব্যালেন্স কম!";
            String msg = "বর্তমান ব্যালেন্স: ৳ " + balance +
                    "\nনির্ধারিত সীমা: ৳ " + meter.lowBalanceThreshold +
                    "\nঅনুগ্রহ করে দ্রুত রিচার্জ করুন।";

            // Decorator pattern — urgent notification for low balance
            new UrgentNotificationDecorator(
                    new BasicNotificationSender(getApplicationContext()),
                    getApplicationContext()
            ).send(title, msg, "low_balance_" + meter.id);
        }
    }
}