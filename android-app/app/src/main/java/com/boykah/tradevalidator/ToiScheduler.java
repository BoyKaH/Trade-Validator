package com.boykah.tradevalidator;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ToiScheduler {
    static final String CHANNEL_ID = "toi_alerts";
    private static final String PREFS = "toi_notifications";
    private static final String PREF_PAYLOAD = "payload";
    private static final String PREF_COUNT = "scheduled_count";
    private static final String PREF_SCHEDULED_IDS = "scheduled_ids";
    private static final String ALARM_ACTION = "com.boykah.tradevalidator.TOI_ALARM";
    private static final int MAX_SAVED_ALERTS = 10000;
    private static final long SCHEDULE_HORIZON = 16L * 24L * 60L * 60L * 1000L;

    private ToiScheduler() {}

    static synchronized JSONObject schedule(Context context, String payloadText) {
        JSONObject result = new JSONObject();
        try {
            JSONObject payload = new JSONObject(payloadText);
            JSONArray input = payload.optJSONArray("alerts");
            if (input == null) throw new JSONException("alerts manquantes");

            cancelAll(context);
            JSONArray savedAlerts = new JSONArray();
            JSONArray scheduledIds = new JSONArray();
            int scheduled = 0;
            long now = System.currentTimeMillis();
            long latest = now + SCHEDULE_HORIZON;
            for (int index = 0; index < Math.min(input.length(), MAX_SAVED_ALERTS); index++) {
                JSONObject raw = input.optJSONObject(index);
                if (raw == null) continue;
                String id = clean(raw.optString("id"), 120);
                String title = clean(raw.optString("title"), 120);
                String body = clean(raw.optString("body"), 240);
                long at = raw.optLong("at", 0L);
                if (id.isEmpty() || title.isEmpty() || at <= now + 3000L) continue;

                JSONObject saved = new JSONObject();
                saved.put("id", id);
                saved.put("title", title);
                saved.put("body", body);
                saved.put("at", at);
                savedAlerts.put(saved);
                if (at <= latest) {
                    scheduleOne(context, id, title, body, at);
                    scheduledIds.put(id);
                    scheduled++;
                }
            }

            JSONObject savedPayload = new JSONObject();
            savedPayload.put("version", 1);
            savedPayload.put("alerts", savedAlerts);
            preferences(context).edit()
                .putString(PREF_PAYLOAD, savedPayload.toString())
                .putString(PREF_SCHEDULED_IDS, scheduledIds.toString())
                .putInt(PREF_COUNT, scheduled)
                .apply();
            scheduleMaintenance(context);

            result.put("available", true);
            result.put("scheduledCount", scheduled);
            result.put("notificationPermission", hasNotificationPermission(context));
            result.put("exactAlarmPermission", hasExactAlarmPermission(context));
        } catch (Exception error) {
            try {
                result.put("available", true);
                result.put("scheduledCount", 0);
                result.put("error", clean(error.getMessage(), 160));
            } catch (JSONException ignored) {
                // JSONObject vide impossible à enrichir uniquement en cas de manque mémoire.
            }
        }
        return result;
    }

    static synchronized void restore(Context context) {
        String payload = preferences(context).getString(PREF_PAYLOAD, "");
        if (!payload.isEmpty()) schedule(context, payload);
    }

    static synchronized void cancelAll(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        String scheduledIdsText = preferences(context).getString(PREF_SCHEDULED_IDS, "[]");
        try {
            JSONArray scheduledIds = new JSONArray(scheduledIdsText);
            for (int index = 0; index < scheduledIds.length(); index++) {
                String id = scheduledIds.optString(index);
                if (!id.isEmpty()) manager.cancel(alarmPendingIntent(context, id, null, null));
            }
        } catch (JSONException ignored) {
            // Une ancienne liste invalide est simplement oubliée.
        }
        manager.cancel(maintenancePendingIntent(context));
        preferences(context).edit().remove(PREF_PAYLOAD).remove(PREF_SCHEDULED_IDS).putInt(PREF_COUNT, 0).apply();
    }

    static JSONObject notificationStatus(Context context) {
        JSONObject status = new JSONObject();
        try {
            status.put("available", true);
            status.put("notificationPermission", hasNotificationPermission(context));
            status.put("exactAlarmPermission", hasExactAlarmPermission(context));
            status.put("scheduledCount", preferences(context).getInt(PREF_COUNT, 0));
            status.put("sdk", Build.VERSION.SDK_INT);
        } catch (JSONException ignored) {
            // Les valeurs sont toutes primitives et ne peuvent normalement pas échouer.
        }
        return status;
    }

    static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    static void postNotification(Context context, String id, String title, String body) {
        ensureNotificationChannel(context);
        if (!hasNotificationPermission(context)) return;

        Intent open = new Intent(context, MainActivity.class);
        open.putExtra(MainActivity.EXTRA_OPEN_SCHEDULE, true);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
            context,
            stableCode("open-" + id),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(context, CHANNEL_ID)
            : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(clean(title, 120))
            .setContentText(clean(body, 240))
            .setStyle(new Notification.BigTextStyle().bigText(clean(body, 240)))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setWhen(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) builder.setPriority(Notification.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(stableCode(id), builder.build());
    }

    private static void scheduleOne(Context context, String id, String title, String body, long at) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = alarmPendingIntent(context, id, title, body);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent);
        } else {
            manager.setExact(AlarmManager.RTC_WAKEUP, at, pendingIntent);
        }
    }

    private static void scheduleMaintenance(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long firstRun = System.currentTimeMillis() + 12L * 60L * 60L * 1000L;
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstRun, AlarmManager.INTERVAL_DAY, maintenancePendingIntent(context));
    }

    private static PendingIntent maintenancePendingIntent(Context context) {
        Intent intent = new Intent(context, ToiRefreshReceiver.class);
        intent.setAction("com.boykah.tradevalidator.REFRESH_TOI_ALARMS");
        return PendingIntent.getBroadcast(
            context,
            918274,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent alarmPendingIntent(Context context, String id, String title, String body) {
        Intent intent = new Intent(context, ToiAlarmReceiver.class);
        intent.setAction(ALARM_ACTION);
        intent.setData(Uri.parse("tradevalidator://toi/" + Uri.encode(id)));
        intent.putExtra("notification_id", id);
        if (title != null) intent.putExtra("title", title);
        if (body != null) intent.putExtra("body", body);
        return PendingIntent.getBroadcast(
            context,
            stableCode(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager.canScheduleExactAlarms();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int stableCode(String value) {
        return value.hashCode() & 0x7fffffff;
    }

    private static String clean(String value, int maxLength) {
        if (value == null) return "";
        String clean = value.trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }
}
