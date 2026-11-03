package com.boykah.tradevalidator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ToiAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("notification_id");
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        if (id == null || title == null) return;
        ToiScheduler.postNotification(context, id, title, body == null ? "" : body);
    }
}
