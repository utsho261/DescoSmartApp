package com.example.descosmartapp.pattern.decorator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import com.example.descosmartapp.R;
import com.example.descosmartapp.ui.MainActivity;

/** DECORATOR — Adds urgency (vibration + high priority) for low balance */
public class UrgentNotificationDecorator implements NotificationSender {

    private final NotificationSender wrapped;
    private final Context context;
    private static final String URGENT_CHANNEL = "desco_urgent";
    private static int urgentId = 2000;

    public UrgentNotificationDecorator(NotificationSender wrapped, Context context) {
        this.wrapped = wrapped;
        this.context = context;
        createUrgentChannel();
    }

    private void createUrgentChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    URGENT_CHANNEL, "⚠️ জরুরি সতর্কতা", NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 500, 200, 500});
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void send(String title, String message, String tag) {
        // Vibrate device
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(new long[]{0,400,200,400}, -1));
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, URGENT_CHANNEL)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("⚠️ " + title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(false)  // stays until dismissed
                .setOngoing(true)      // persistent for low balance
                .setContentIntent(pi);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(tag, urgentId++, builder.build());
    }
}