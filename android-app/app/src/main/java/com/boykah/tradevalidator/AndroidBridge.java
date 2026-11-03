package com.boykah.tradevalidator;

import android.webkit.JavascriptInterface;

public final class AndroidBridge {
    private final MainActivity activity;

    AndroidBridge(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public String getNotificationStatus() {
        return ToiScheduler.notificationStatus(activity).toString();
    }

    @JavascriptInterface
    public String scheduleToiNotifications(String payload) {
        return ToiScheduler.schedule(activity, payload).toString();
    }

    @JavascriptInterface
    public void cancelToiNotifications() {
        ToiScheduler.cancelAll(activity);
    }

    @JavascriptInterface
    public void requestNotificationPermission() {
        activity.runOnUiThread(activity::requestNotificationPermission);
    }

    @JavascriptInterface
    public void requestExactAlarmPermission() {
        activity.runOnUiThread(activity::requestExactAlarmPermission);
    }

    @JavascriptInterface
    public void openNotificationSettings() {
        activity.runOnUiThread(activity::openAppNotificationSettings);
    }

    @JavascriptInterface
    public void showTestNotification(String title, String body) {
        ToiScheduler.postNotification(activity, "toi-test", title, body);
    }
}
