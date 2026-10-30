package com.redcreep.glasslock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

public class LockService extends Service {

    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            launch(context);
        }
    };

    public static void start(Context c) {
        Intent i = new Intent(c, LockService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            c.startForegroundService(i);
        } else {
            c.startService(i);
        }
    }

    public static void launch(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.putExtra("lock", true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        c.startActivity(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("lock", "Glass Lock", NotificationManager.IMPORTANCE_MIN));
            b = new Notification.Builder(this, "lock");
        } else {
            b = new Notification.Builder(this);
        }
        b.setContentTitle("Glass Lock is active");
        b.setSmallIcon(android.R.drawable.ic_lock_idle_lock);
        startForeground(1, b.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(screenOff);
        } catch (Exception e) {
            // already unregistered
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
