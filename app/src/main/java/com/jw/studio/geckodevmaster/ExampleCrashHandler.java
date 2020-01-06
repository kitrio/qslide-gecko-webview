package com.jw.studio.geckodevmaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import org.mozilla.geckoview.BuildConfig;
import org.mozilla.geckoview.CrashReporter;
import org.mozilla.geckoview.GeckoRuntime;

public class ExampleCrashHandler extends Service {
    private static final String LOGTAG = "Qwebview CrashHandler";

    private static final String CHANNEL_ID = "qwebview_crashes";
    private static final int NOTIFY_ID = 42;

    private static final String ACTION_REPORT_CRASH = "org.mozilla.geckoview_example.ACTION_REPORT_CRASH";
    private static final String ACTION_DISMISS = "org.mozilla.geckoview_example.ACTION_DISMISS";

    public ExampleCrashHandler() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        if (GeckoRuntime.ACTION_CRASHED.equals(intent.getAction())) {

            String id = createNotificationChannel();

            PendingIntent reportIntent = PendingIntent.getService(
                    this, 0,
                    new Intent(ACTION_REPORT_CRASH, null,
                            this, ExampleCrashHandler.class), 0);

            PendingIntent dismissIntent = PendingIntent.getService(
                    this, 0,
                    new Intent(ACTION_DISMISS, null,
                            this, ExampleCrashHandler.class), 0);

            Notification notification = new NotificationCompat.Builder(this, id)
                    .setSmallIcon(R.drawable.ic_crash)
                    .setContentTitle(getResources().getString(R.string.crashed_title))
                    .setContentText(getResources().getString(R.string.crashed_text))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setContentIntent(reportIntent)
                    .addAction(0, getResources().getString(R.string.crashed_ignore), dismissIntent)
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .build();

            startForeground(NOTIFY_ID, notification);
        } else if (ACTION_REPORT_CRASH.equals(intent.getAction())) {
            StrictMode.ThreadPolicy oldPolicy = null;
            if (BuildConfig.DEBUG) {
                oldPolicy = StrictMode.getThreadPolicy();

                // We do some disk I/O and network I/O on the main thread, but it's fine.
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(oldPolicy)
                        .permitDiskReads()
                        .permitDiskWrites()
                        .permitNetwork()
                        .build());
            }

            if (oldPolicy != null) {
                StrictMode.setThreadPolicy(oldPolicy);
            }

            stopSelf();
        } else if (ACTION_DISMISS.equals(intent.getAction())) {
            stopSelf();
        }

        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "QWebview Crashes", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            return CHANNEL_ID;
        }
        return "";
    }

}
