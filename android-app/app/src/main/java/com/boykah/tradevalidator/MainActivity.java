package com.boykah.tradevalidator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

public class MainActivity extends android.app.Activity {
    static final String APP_URL = "https://boykah.github.io/Trade-Validator/";
    static final String EXTRA_OPEN_SCHEDULE = "open_schedule";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private static final int FILE_CHOOSER_REQUEST = 1002;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private boolean openScheduleAfterLoad;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ToiScheduler.ensureNotificationChannel(this);
        openScheduleAfterLoad = getIntent().getBooleanExtra(EXTRA_OPEN_SCHEDULE, false);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFF0F0F0F);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " TradeValidatorAndroid/1.0");

        webView.addJavascriptInterface(new AndroidBridge(this), "TradeValidatorAndroid");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent chooser;
                try {
                    chooser = params.createIntent();
                    startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException error) {
                    filePathCallback = null;
                    return false;
                }
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (isTrustedAppUrl(uri)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) {
                    // Aucun navigateur disponible : la page principale reste ouverte.
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                dispatchWebEvent("android-bridge-ready");
                if (openScheduleAfterLoad) {
                    openScheduleAfterLoad = false;
                    view.evaluateJavascript("var b=document.getElementById('schedule-open');if(b)b.click()", null);
                }
            }
        });
        webView.loadUrl(APP_URL);
    }

    private boolean isTrustedAppUrl(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        if (!"boykah.github.io".equalsIgnoreCase(uri.getHost())) return false;
        String path = uri.getPath() == null ? "/" : uri.getPath();
        return path.toLowerCase(Locale.ROOT).startsWith("/trade-validator/");
    }

    void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        } else {
            dispatchWebEvent("android-notification-permission-changed");
        }
    }

    void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            dispatchWebEvent("android-notification-permission-changed");
            return;
        }
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (manager.canScheduleExactAlarms()) {
            dispatchWebEvent("android-notification-permission-changed");
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            openAppNotificationSettings();
        }
    }

    void openAppNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    void dispatchWebEvent(String eventName) {
        if (webView == null) return;
        webView.post(() -> webView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent(" + quoteForJavaScript(eventName) + "))", null
        ));
    }

    private String quoteForJavaScript(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    protected void onResume() {
        super.onResume();
        dispatchWebEvent("android-notification-permission-changed");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra(EXTRA_OPEN_SCHEDULE, false) && webView != null) {
            webView.evaluateJavascript("var b=document.getElementById('schedule-open');if(b)b.click()", null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            dispatchWebEvent("android-notification-permission-changed");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;
        filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        filePathCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("TradeValidatorAndroid");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
