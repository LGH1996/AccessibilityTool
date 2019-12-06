package com.lgh.accessibilitytool;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

public class HelpActivity extends Activity {

    static final String AUTO_REMOVE = "autoRemove";
    private SharedPreferences sharedPreferences;
    private boolean autoRemove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        autoRemove = sharedPreferences.getBoolean(AUTO_REMOVE, true);
        WebView webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "HelpActivity");
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(webView.getLayoutParams());
        params.topMargin = resources.getDimensionPixelSize(resourceId) + 5;
        webView.setLayoutParams(params);
        webView.loadUrl("file:///android_asset/help.html");
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            sharedPreferences.edit().putBoolean(AUTO_REMOVE, autoRemove).apply();
            if (autoRemove) {
                finishAndRemoveTask();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferences.edit().putBoolean(AUTO_REMOVE, autoRemove).apply();
        if (autoRemove) {
            finishAndRemoveTask();
        }
    }

    @JavascriptInterface
    public boolean getAutoRemove() {
        return autoRemove;
    }

    @JavascriptInterface
    public void setAutoRemove(boolean b) {
        autoRemove = b;
    }

    @JavascriptInterface
    public void chooseService() {
        Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent_abs);
    }
}
