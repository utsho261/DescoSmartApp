package com.example.descosmartapp.worker;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Schedules balance checks 5 times daily:
 * 7am, 10am, 1pm, 5pm, 9pm
 */
public class WorkScheduler {

    private static final int[] CHECK_HOURS = {7, 10, 13, 17, 21};

    public static void scheduleAll(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelAllWorkByTag("balance_check");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        for (int hour : CHECK_HOURS) {
            long delay = calcDelay(hour);
            OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(BalanceCheckWorker.class)
                    .setConstraints(constraints)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag("balance_check")
                    .build();
            wm.enqueue(req);
        }
    }

    private static long calcDelay(int targetHour) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, targetHour);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1); // next day
        }
        return target.getTimeInMillis() - now.getTimeInMillis();
    }
}