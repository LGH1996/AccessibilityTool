package com.lgh.accessibilitytool;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityServiceNoGesture extends AccessibilityService {

    private int create_num, connect_num;
    public static MainFunctions mainFunctions;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            create_num = 0;
            connect_num = 0;
            create_num++;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (++connect_num != create_num) {
            throw new RuntimeException("无障碍服务出现异常");
        }
        mainFunctions = new MainFunctions(this);
        mainFunctions.onServiceConnected();
        if (MyAccessibilityService.mainFunctions != null) {
            MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x04);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        mainFunctions.onAccessibilityEvent(event);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return mainFunctions.onKeyEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mainFunctions.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mainFunctions.onUnbind(intent);
        mainFunctions = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
    }
}
