package com.lgh.accessibilitytool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyScreenOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        try {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    if (MyAccessibilityService.mainFunctions != null) {
                        MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x05);
                    }
                    if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                        MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x05);
                    }
                }
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    if (MyAccessibilityService.mainFunctions != null) {
                        MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x03);
                    }
                    if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                        MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x03);
                    }
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
