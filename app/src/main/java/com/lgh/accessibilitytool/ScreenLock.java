package com.lgh.accessibilitytool;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class ScreenLock {
    private Context context;
    private DevicePolicyManager devicePolicyManager;
    private WindowManager windowManager;
    private ImageView imageView;
    public ScreenLock(Context context){
        this.context=context;
        devicePolicyManager=(DevicePolicyManager)context.getSystemService(context.DEVICE_POLICY_SERVICE);
        windowManager=(WindowManager) context.getSystemService(context.WINDOW_SERVICE);
    }

    public void showLockFloat(){
        if (imageView!=null) return;
        WindowManager.LayoutParams params=new WindowManager.LayoutParams();
        params.type=WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        params.flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.format= PixelFormat.TRANSPARENT;
        params.gravity= Gravity.START|Gravity.TOP;
        params.alpha=0f;
        params.y=15;
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        params.height= resources.getDimensionPixelSize(resourceId)-15;
        imageView=new ImageView(context);
        imageView.setOnClickListener(new View.OnClickListener() {
            long start=System.currentTimeMillis();
            @Override
            public void onClick(View v) {
                long end=System.currentTimeMillis();
                if ((end-start)<500){
                    devicePolicyManager.lockNow();
                }
                start=end;
            }
        });
        windowManager.addView(imageView,params);
    }

    public void dismiss(){
        if (imageView!=null) {
            windowManager.removeViewImmediate(imageView);
            imageView=null;
        }
    }

}
