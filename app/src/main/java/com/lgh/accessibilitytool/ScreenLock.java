package com.lgh.accessibilitytool;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class ScreenLock {
    private Context context;
    private DevicePolicyManager devicePolicyManager;
    private WindowManager windowManager;
    private ImageView imageView_left, imageView_right;

    public ScreenLock(Context context) {
        this.context = context;
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(context.DEVICE_POLICY_SERVICE);
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
    }

    public void showLockFloat() {
        if (imageView_left != null && imageView_right != null) return;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.format = PixelFormat.TRANSPARENT;
        params.alpha = 0f;
        params.y = 10;
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        params.width = resources.getDisplayMetrics().widthPixels / 10;
        params.height = resources.getDimensionPixelSize(resourceId) - 20;
        imageView_left = new ImageView(context);
        imageView_right = new ImageView(context);
        View.OnClickListener clickListener = new View.OnClickListener() {
            long start = System.currentTimeMillis();

            @Override
            public void onClick(View v) {
                long end = System.currentTimeMillis();
                if ((end - start) < 800) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        MyAccessibilityService.handler.sendEmptyMessage(0x01);
                    } else {
                        devicePolicyManager.lockNow();
                    }
                }
                start = end;
            }
        };
        imageView_left.setOnClickListener(clickListener);
        imageView_right.setOnClickListener(clickListener);
        params.gravity = Gravity.START | Gravity.TOP;
        windowManager.addView(imageView_left, params);
        params.gravity = Gravity.END | Gravity.TOP;
        windowManager.addView(imageView_right, params);
    }

    public void dismiss() {
        if (imageView_left != null) {
            windowManager.removeViewImmediate(imageView_left);
            imageView_left = null;

        }
        if (imageView_right != null) {
            windowManager.removeViewImmediate(imageView_right);
            imageView_right = null;
        }
    }

}
