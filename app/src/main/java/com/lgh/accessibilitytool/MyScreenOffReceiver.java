package com.lgh.accessibilitytool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyScreenOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (MyAccessibilityService.handler!=null)
        MyAccessibilityService.handler.sendEmptyMessage(0x03);
    }
}
