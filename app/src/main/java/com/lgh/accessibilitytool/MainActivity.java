package com.lgh.accessibilitytool;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {
//    static String TAG= "MainActivity";
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            context=getApplicationContext();
            String str= Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (str!=null&&str.contains(MyAccessibilityService.class.getName())&&MyAccessibilityService.handler!=null) {
                if (Settings.canDrawOverlays(context)){
                MyAccessibilityService.handler.sendEmptyMessage(0x00);
                Toast.makeText(context, "请确保允许应用后台运行", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "请打开＜无障碍＞服务", Toast.LENGTH_SHORT).show();
                Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent_abs);
            }

            if (!Settings.canDrawOverlays(context)){
                Intent intent_dol = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));
                intent_dol.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent_dol);
                Toast.makeText(context, "请打开应用＜悬浮窗＞权限,并设置允许后台运行", Toast.LENGTH_SHORT).show();
            }


            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
                Intent intent_sto = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                intent_sto.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent_sto);
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Toast.makeText(context, "请授予＜读写手机存储＞权限，,并设置允许后台运行", Toast.LENGTH_SHORT).show();
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

    private void displayNotification() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("NTFID", "AccessibilityService", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(context, "NTFID");
        } else {
            builder = new Notification.Builder(context);
        }

        builder.addAction(new Notification.Action.Builder(null, "无障碍权限", PendingIntent.getActivity(context, 0x00, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), PendingIntent.FLAG_CANCEL_CURRENT)).build());
        builder.addAction(new Notification.Action.Builder(null, "关闭", PendingIntent.getBroadcast(context, 0x01, new Intent("android.intent.action.SERVICE_CLOSE"), PendingIntent.FLAG_UPDATE_CURRENT)).build());

        PendingIntent pendingIntent=PendingIntent.getActivity(context,0,new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification = builder.setSmallIcon(R.drawable.a).setContentText("请确保无障碍服务已打开，点击进入应用设置").setContentIntent(pendingIntent).setShowWhen(false).setAutoCancel(false).build();
        notification.flags=Notification.FLAG_NO_CLEAR;
        notificationManager.cancelAll();
        notificationManager.notify(0x00, notification);
    }
}
