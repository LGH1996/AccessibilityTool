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
            PackageManager packageManager = getPackageManager();

            if (MyAccessibilityService.handler != null) {
                if (Settings.canDrawOverlays(context)) {
                    MyAccessibilityService.handler.sendEmptyMessage(0x00);
                }
            } else {
                Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent_abs);
                Toast.makeText(context, "请打开＜无障碍＞服务", Toast.LENGTH_SHORT).show();
            }

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                openAppSet();
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Toast.makeText(context, "请授予＜读写手机存储＞权限，,并设置允许后台运行", Toast.LENGTH_SHORT).show();
            }

            if (!Settings.canDrawOverlays(context)) {
                Intent intent_dol = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                intent_dol.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                ResolveInfo resolveInfo = packageManager.resolveActivity(intent_dol, PackageManager.MATCH_ALL);
                if (resolveInfo != null) {
                    startActivity(intent_dol);
                } else {
                    openAppSet();
                }
                Toast.makeText(context, "请打开应用＜悬浮窗＞权限,并设置允许后台运行", Toast.LENGTH_SHORT).show();
            }

            if (!((PowerManager) getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent_ibo = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()));
                intent_ibo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                ResolveInfo resolveInfo = packageManager.resolveActivity(intent_ibo, PackageManager.MATCH_ALL);
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
        finish();
    }

    private void openAppSet() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
