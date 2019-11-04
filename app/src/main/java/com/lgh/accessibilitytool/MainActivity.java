package com.lgh.accessibilitytool;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {

    static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Context context = getApplicationContext();

            if (MyAccessibilityService.mainFunctions == null && MyAccessibilityServiceNoGesture.mainFunctions == null) {
                Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent_abs.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent_abs);
                Toast.makeText(context, "请根据情况打开其中一个无障碍服务", Toast.LENGTH_SHORT).show();
            } else if (MyAccessibilityService.mainFunctions != null && MyAccessibilityServiceNoGesture.mainFunctions != null) {
                Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent_abs.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent_abs);
                Toast.makeText(context, "无障碍服务冲突，请根据情况关闭其中一个", Toast.LENGTH_SHORT).show();
            } else {

                if (MyAccessibilityService.mainFunctions != null) {
                    MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x00);
                }

                if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                    MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x00);
                }
            }

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Toast.makeText(context, "请授予＜读写手机存储＞权限，,并设置允许后台运行", Toast.LENGTH_SHORT).show();
            }

            if (!((PowerManager) getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent_ibo = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()));
                intent_ibo.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent_ibo, PackageManager.MATCH_ALL);
                if (resolveInfo != null)
                    startActivity(intent_ibo);
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        finishAndRemoveTask();
    }
}
