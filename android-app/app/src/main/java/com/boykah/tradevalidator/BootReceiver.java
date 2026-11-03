package com.boykah.tradevalidator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pending = goAsync();
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                ToiScheduler.restore(appContext);
            } finally {
                pending.finish();
            }
        }, "toi-alarm-restore").start();
    }
}
