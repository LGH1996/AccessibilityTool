package com.lgh.accessibilitytool;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class HelpActivity extends Activity {

    static final String AUTO_REMOVE = "autoRemove";
    private SharedPreferences sharedPreferences;
    private boolean autoRemove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        autoRemove = sharedPreferences.getBoolean(AUTO_REMOVE, true);
        setContentView(R.layout.activity_help);
        WebView webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "HelpActivity");
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(webView.getLayoutParams());
        params.topMargin = resources.getDimensionPixelSize(resourceId);
        webView.setLayoutParams(params);
        webView.loadUrl("file:///android_asset/help.html");
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
        Toast.makeText(this, "getAutoRemove", Toast.LENGTH_SHORT).show();
        return autoRemove;
    }

    @JavascriptInterface
    public void setAutoRemove(boolean b) {
        Toast.makeText(this, "setAutoRemove" + b, Toast.LENGTH_SHORT).show();
        autoRemove = b;
    }
}
