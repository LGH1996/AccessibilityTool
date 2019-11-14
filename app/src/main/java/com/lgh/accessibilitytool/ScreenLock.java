package com.lgh.accessibilitytool;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;

public class ScreenLock {

    private static String WIDTH = "ScreenLock_Width";
    private static String HEIGHT = "ScreenLock_Height";
    private static String POSITION_X = "ScreenLock_PositionX";
    private static String POSITION_Y = "ScreenLock_PositionY";
    private int width, height, px, py;
    private Context context;
    private DevicePolicyManager devicePolicyManager;
    private SharedPreferences sharedPreferences;
    private WindowManager windowManager;
    private DisplayMetrics metrics;
    private ImageView imageView;
    private WindowManager.LayoutParams params;
    private View view;

    public ScreenLock(Context context) {
        this.context = context;
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        width = sharedPreferences.getInt(WIDTH, metrics.widthPixels / 20);
        height = sharedPreferences.getInt(HEIGHT, metrics.widthPixels / 20);
        px = sharedPreferences.getInt(POSITION_X, metrics.widthPixels - width);
        py = sharedPreferences.getInt(POSITION_Y, metrics.heightPixels - height);
    }

    public void showLockFloat() {
        if (imageView != null) return;
        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.format = PixelFormat.TRANSPARENT;
        params.gravity = Gravity.START | Gravity.TOP;
        params.alpha = 0.5f;
        params.width = width;
        params.height = height;
        params.x = px;
        params.y = py;
        imageView = new ImageView(context);
        imageView.setBackgroundColor(0x00000000);
        View.OnClickListener clickListener = new View.OnClickListener() {
            long start = System.currentTimeMillis();

            @Override
            public void onClick(View v) {
                long end = System.currentTimeMillis();
                if ((end - start) < 800) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (MyAccessibilityService.mainFunctions != null) {
                            MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x01);
                        }
                        if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                            MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x01);
                        }
                    } else {
                        devicePolicyManager.lockNow();
                    }
                }
                start = end;
            }
        };
        imageView.setOnClickListener(clickListener);
        windowManager.addView(imageView, params);
    }

    public void dismiss() {
        if (imageView != null) {
            windowManager.removeViewImmediate(imageView);
            imageView = null;
            params = null;
        }
        if (view != null) {
            view.findViewById(R.id.seekBar_w).setEnabled(false);
            view.findViewById(R.id.seekBar_h).setEnabled(false);
            view.findViewById(R.id.seekBar_x).setEnabled(false);
            view.findViewById(R.id.seekBar_y).setEnabled(false);
        }
    }

    public void showSetAreaDialog() {
        view = LayoutInflater.from(context).inflate(R.layout.screen_lock_position, null);
        SeekBar seekBar_W = view.findViewById(R.id.seekBar_w);
        SeekBar seekBar_H = view.findViewById(R.id.seekBar_h);
        SeekBar seekBar_X = view.findViewById(R.id.seekBar_x);
        SeekBar seekBar_Y = view.findViewById(R.id.seekBar_y);
        seekBar_W.setMax(metrics.widthPixels / 4);
        seekBar_H.setMax(metrics.heightPixels / 4);
        seekBar_X.setMax(metrics.widthPixels);
        seekBar_Y.setMax(metrics.heightPixels);
        seekBar_W.setProgress(width);
        seekBar_H.setProgress(height);
        seekBar_X.setProgress(px);
        seekBar_Y.setProgress(py);
        if (imageView == null) {
            seekBar_W.setEnabled(false);
            seekBar_H.setEnabled(false);
            seekBar_X.setEnabled(false);
            seekBar_Y.setEnabled(false);
        }
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (imageView == null || params == null) return;
                switch (seekBar.getId()) {
                    case R.id.seekBar_w:
                        params.width = i;
                        break;
                    case R.id.seekBar_h:
                        params.height = i;
                        break;
                    case R.id.seekBar_x:
                        params.x = i;
                        break;
                    case R.id.seekBar_y:
                        params.y = i;
                        break;
                }
                windowManager.updateViewLayout(imageView, params);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        seekBar_W.setOnSeekBarChangeListener(onSeekBarChangeListener);
        seekBar_H.setOnSeekBarChangeListener(onSeekBarChangeListener);
        seekBar_X.setOnSeekBarChangeListener(onSeekBarChangeListener);
        seekBar_Y.setOnSeekBarChangeListener(onSeekBarChangeListener);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(view).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (imageView != null && params != null) {
                    width = params.width;
                    height = params.height;
                    px = params.x;
                    py = params.y;
                    sharedPreferences.edit().putInt(WIDTH, width).putInt(HEIGHT, height).putInt(POSITION_X, px).putInt(POSITION_Y, py).apply();
                    imageView.setBackgroundColor(0x00000000);
                }
                view = null;
            }
        }).create();
        Window win = dialog.getWindow();
        win.setBackgroundDrawableResource(R.drawable.dialogbackground);
        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        win.setDimAmount(0);
        dialog.show();
        WindowManager.LayoutParams win_params = win.getAttributes();
        if (metrics.heightPixels > metrics.widthPixels) {
            win_params.width = (metrics.widthPixels / 6) * 5;
            win_params.height = metrics.heightPixels / 2;
        } else {
            win_params.width = (metrics.heightPixels / 6) * 5;
            win_params.height = metrics.widthPixels / 2;
        }
        win.setAttributes(win_params);
        if (imageView != null)
            imageView.setBackgroundColor(0xffff0000);
    }
}
