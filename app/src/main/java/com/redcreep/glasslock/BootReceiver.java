package com.redcreep.glasslock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context.getSharedPreferences("g", Context.MODE_PRIVATE).getBoolean("on", false)) {
            LockService.start(context);
        }
    }
}
