package com.lgh.accessibilitytool;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MyAccessibilityService extends AccessibilityService {

    static final String TAG = "MyAccessibilityService";
    static final String CONTROL_LIGHTNESS = "control_lightness";
    static final String CONTROL_LOCK = "control_lock";
    static final String RECORD_CLIP = "record_clip";
    static final String EVENT_TYPES = "event_type";
    static final String FLAGS = "flags";
    static final String PAC_MSG = "pac_msg";
    static final String VIBRATION_STRENGTH = "vibration_strength";
    static final String ACTIVITY_MAP = "act_p";
    static final String PAC_WHITE = "pac_white";
    static final String KEY_WORD_LIST = "keyWordList";
    public static Handler handler;
    private boolean double_press;
    private boolean is_release_up, is_release_down;
    private boolean control_lightness, control_lock, is_state_change, record_clip;
    private long star_up, star_down;
    private int win_state_count, create_num, connect_num, vibration_strength;
    private SharedPreferences sharedPreferences;
    private ScheduledFuture future;
    private ScheduledExecutorService executorService;
    private AudioManager audioManager;
    private PackageManager packageManager;
    private Vibrator vibrator;
    private Set<String> pac_msg, pac_launch, pac_white, pac_home, pac_input;
    private ArrayList<String> keyWordList;
    private Map<String, SkipButtonDescribe> act_p;
    private AccessibilityServiceInfo asi;
    private String old_pac, cur_act, savePath, packageName;
    private WindowManager windowManager;
    private DevicePolicyManager devicePolicyManager;
    private ScreenLightness screenLightness;
    private MediaButtonControl mediaButtonControl;
    private ScreenLock screenLock;
    private MyInstallReceiver installReceiver;
    private MyScreenOffReceiver screenOnReceiver;
    private ClipboardManager clipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener primaryClipChangedListener;
    private WindowManager.LayoutParams aParams;
    private WindowManager.LayoutParams bParams;
    private View adv_view;
    private ImageView target_xy;

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
            throw null;
        }
        try {
            initializeData();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        Log.i(TAG,AccessibilityEvent.eventTypeToString(event.getEventType())+"|"+event.getPackageName()+"|"+event.getClassName());
        try {
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    String str = event.getPackageName().toString();
                    if (!str.equals(old_pac)) {
                        if (pac_launch.contains(str)) {
                            asi.eventTypes |= AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                            setServiceInfo(asi);
                            is_state_change = true;
                            win_state_count = 0;
                            old_pac = str;
                            future.cancel(false);
                            future = executorService.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                                    setServiceInfo(asi);
                                    is_state_change = false;
                                }
                            }, 8000, TimeUnit.MILLISECONDS);
                        } else if (pac_white.contains(str)) {
                            old_pac = str;
                            if (is_state_change) {
                                asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                                setServiceInfo(asi);
                                is_state_change = false;
                                future.cancel(false);
                            }
                        }
                    }
                    if (is_state_change && str.equals(old_pac)) {
                        findSkipButton(getRootInActiveWindow());
                    }
                    String act = event.getClassName().toString();
                    if (!act.startsWith("android.widget.")) {
                        cur_act = act;
                        if (is_state_change) {
                            final SkipButtonDescribe p = act_p.get(act);
                            if (p != null) {
                                click(p.x, p.y, 0, 20);
                                asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                                setServiceInfo(asi);
                                is_state_change = false;
                                future.cancel(false);
                                executorService.scheduleAtFixedRate(new Runnable() {
                                    int num = 0;

                                    @Override
                                    public void run() {
                                        if (act_p.containsKey(cur_act) && num < (8000 / p.period)) {
                                            click(p.x, p.y, 0, 20);
                                            num++;
                                        } else throw new RuntimeException();
                                    }
                                }, 0, p.period, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                    if (is_state_change && !event.getPackageName().equals("com.android.systemui")) {
                        if (win_state_count <= 5) {
                            findSkipButton(getRootInActiveWindow());
                        } else {
                            findSkipButton(event.getSource());
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    if (event.getParcelableData() instanceof Notification && pac_msg.contains(event.getPackageName())) {
                        List<CharSequence> list_msg = event.getText();
                        StringBuilder builder = new StringBuilder();
                        for (CharSequence s : list_msg) {
                            builder.append(s.toString().replaceAll("\\s", ""));
                        }
                        String tem = builder.toString();
                        if (!tem.isEmpty()) {
                            FileWriter writer = new FileWriter(savePath + "/" + "NotificationMessageCache.txt", true);
                            writer.append("[");
                            writer.append(tem);
                            writer.append("]" + "\n");
                            writer.close();
                        }
                    }
                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
//        Log.i(TAG,KeyEvent.keyCodeToString(event.getKeyCode())+"-"+event.getAction());
        try {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    switch (event.getAction()) {
                        case KeyEvent.ACTION_DOWN:
//                            Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_UP -> KeyEvent.ACTION_DOWN");
                            star_up = System.currentTimeMillis();
                            is_release_up = false;
                            double_press = false;
                            if (is_release_down) {
                                future = executorService.schedule(new Runnable() {
                                    @Override
                                    public void run() {
//                                        Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_UP -> THREAD");
                                        if (!is_release_down) {
                                            mediaButtonControl.play_pause_Music();
                                            vibrator.vibrate(vibration_strength);
                                        } else if (!is_release_up && audioManager.isMusicActive()) {
                                            mediaButtonControl.nextMusic();
                                            vibrator.vibrate(vibration_strength);
                                        }
                                    }
                                }, 800, TimeUnit.MILLISECONDS);
                            } else {
                                double_press = true;
                            }
                            break;
                        case KeyEvent.ACTION_UP:
//                            Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_UP -> KeyEvent.ACTION_UP");
                            future.cancel(false);
                            is_release_up = true;
                            if (!double_press && System.currentTimeMillis() - star_up < 800) {
                                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                            }
                            break;
                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    switch (event.getAction()) {
                        case KeyEvent.ACTION_DOWN:
//                            Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_DOWN -> KeyEvent.ACTION_DOWN");
                            star_down = System.currentTimeMillis();
                            is_release_down = false;
                            double_press = false;
                            if (is_release_up) {
                                future = executorService.schedule(new Runnable() {
                                    @Override
                                    public void run() {
//                                        Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_DOWN -> THREAD");
                                        if (!is_release_up) {
                                            mediaButtonControl.play_pause_Music();
                                            vibrator.vibrate(vibration_strength);
                                        } else if (!is_release_down && audioManager.isMusicActive()) {
                                            mediaButtonControl.previousMusic();
                                            vibrator.vibrate(vibration_strength);
                                        }
                                    }
                                }, 800, TimeUnit.MILLISECONDS);

                            } else {
                                double_press = true;
                            }
                            break;
                        case KeyEvent.ACTION_UP:
//                            Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_DOWN -> KeyEvent.ACTION_UP");
                            future.cancel(false);
                            is_release_down = true;
                            if (!double_press && System.currentTimeMillis() - star_down < 800) {
                                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                            }
                            break;
                    }
                    return true;
                default:
//                    Log.i(TAG,KeyEvent.keyCodeToString(event.getKeyCode()));
                    return false;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            if (control_lightness) {
                screenLightness.refreshOnOrientationChange();
            }
            if (control_lock) {
                switch (newConfig.orientation) {
                    case Configuration.ORIENTATION_PORTRAIT:
                        screenLock.showLockFloat();
                        break;
                    case Configuration.ORIENTATION_LANDSCAPE:
                        screenLock.dismiss();
                        break;
                }
            }
            if (adv_view != null && target_xy != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getRealMetrics(metrics);
                aParams.x = (metrics.widthPixels - aParams.width) / 2;
                aParams.y = (metrics.heightPixels - aParams.height) / 2;
                bParams.x = (metrics.widthPixels - bParams.width) / 2;
                bParams.y = metrics.heightPixels - bParams.height;
                windowManager.updateViewLayout(adv_view, bParams);
                windowManager.updateViewLayout(target_xy, aParams);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            unregisterReceiver(installReceiver);
            unregisterReceiver(screenOnReceiver);
            clipboardManager.removePrimaryClipChangedListener(primaryClipChangedListener);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
    }

    /**
     * 用于启动界面查找“跳过”的控件
     */
    private void findSkipButton(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return;
        for (int n = 0; n < keyWordList.size(); n++) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(keyWordList.get(n));
            for (AccessibilityNodeInfo e : list) {
                if (!e.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    if (!e.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Rect rect = new Rect();
                        e.getBoundsInScreen(rect);
                        click(rect.centerX(), rect.centerY(), 0, 20);
                    }
                }
            }
            if (!list.isEmpty() || win_state_count >= 25) {
                asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                setServiceInfo(asi);
                is_state_change = false;
                future.cancel(false);
                break;
            }
        }
        win_state_count++;
    }

    /**
     * 模拟
     * 点击
     */
    private boolean click(int X, int Y, long start_time, long duration) {
        Path path = new Path();
        path.moveTo(X, Y);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            GestureDescription.Builder builder = new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path, start_time, duration));
            return dispatchGesture(builder.build(), null, null);
        } else {
            return false;
        }
    }

    /**
     * 初始化各种数据
     */
    private void initializeData() {
        is_release_up = true;
        is_release_down = true;
        is_state_change = false;
        double_press = false;
        old_pac = "Initialize PackageName";
        cur_act = "Initialize ClassName";
        packageName = getPackageName();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sharedPreferences = getSharedPreferences(packageName, MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        packageManager = getPackageManager();
        executorService = Executors.newSingleThreadScheduledExecutor();
        mediaButtonControl = new MediaButtonControl(this);
        screenLightness = new ScreenLightness(this);
        screenLock = new ScreenLock(this);
        installReceiver = new MyInstallReceiver();
        screenOnReceiver = new MyScreenOffReceiver();
        vibration_strength = sharedPreferences.getInt(VIBRATION_STRENGTH, 50);
        pac_msg = sharedPreferences.getStringSet(PAC_MSG, new HashSet<String>());
        pac_white = sharedPreferences.getStringSet(PAC_WHITE, null);
        control_lock = sharedPreferences.getBoolean(CONTROL_LOCK, true) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || devicePolicyManager.isAdminActive(new ComponentName(this, MyDeviceAdminReceiver.class)));
        control_lightness = sharedPreferences.getBoolean(CONTROL_LIGHTNESS, false);
        record_clip = sharedPreferences.getBoolean(RECORD_CLIP, true);
        if (control_lock) screenLock.showLockFloat();
        if (control_lightness) screenLightness.showFloat();
        asi = getServiceInfo();
        asi.eventTypes = sharedPreferences.getInt(EVENT_TYPES, AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        asi.flags = sharedPreferences.getInt(FLAGS, asi.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
        setServiceInfo(asi);
        updatePackage();
        IntentFilter filter_install = new IntentFilter();
        filter_install.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter_install.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter_install.addDataScheme("package");
        registerReceiver(installReceiver, filter_install);
        IntentFilter filter_screen = new IntentFilter();
        filter_screen.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOnReceiver, filter_screen);
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if (!file.exists()) file.mkdirs();
        savePath = file.getAbsolutePath();
        primaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            String old_clip = "";

            @Override
            public void onPrimaryClipChanged() {
                try {
                    if (old_pac.equals(packageName)) return;
                    ClipData clipData = clipboardManager.getPrimaryClip();
                    if (clipData == null) return;
                    StringBuilder builder = new StringBuilder();
                    for (int n = 0; n < clipData.getItemCount(); n++) {
                        ClipData.Item item = clipData.getItemAt(n);
                        CharSequence str = item.getText();
                        if (str == null) continue;
                        builder.append(str.toString().replaceAll("\\s", ""));
                    }
                    String text = builder.toString();
                    if (text.isEmpty() || text.equals(old_clip)) return;
                    old_clip = text;
                    FileWriter writer = new FileWriter(savePath + "/" + "ClipContentCache.txt", true);
                    writer.append(text);
                    writer.append("\n");
                    writer.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
        if (record_clip) {
            clipboardManager.addPrimaryClipChangedListener(primaryClipChangedListener);
        }
        String aJson = sharedPreferences.getString(ACTIVITY_MAP, null);
        if (aJson != null) {
            Type type = new TypeToken<HashMap<String, SkipButtonDescribe>>() {
            }.getType();
            act_p = new Gson().fromJson(aJson, type);
        } else {
            act_p = new HashMap<>();
        }
        String bJson = sharedPreferences.getString(KEY_WORD_LIST, null);
        if (bJson != null) {
            Type type = new TypeToken<ArrayList<String>>() {
            }.getType();
            keyWordList = new Gson().fromJson(bJson, type);
        } else {
            keyWordList = new ArrayList<>();
            keyWordList.add("跳过");
        }
        future = executorService.schedule(new Runnable() {
            @Override
            public void run() {
            }
        }, 0, TimeUnit.MILLISECONDS);
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 0x00:
                        mainUI();
                        break;
                    case 0x01:
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
                        break;
                    case 0x02:
                        updatePackage();
                        mediaButtonControl.updateMusicSet();
                        break;
                    case 0x03:
                        old_pac = "ScreenOff PackageName";
                        break;
                }
                return true;
            }
        });
    }

    /**
     * 在安装卸载软件时触发调用，
     * 更新相关包名的集合
     */
    private void updatePackage() {

        Intent intent;
        List<ResolveInfo> ResolveInfoList;
        pac_launch = new HashSet<>();
        pac_home = new HashSet<>();
        pac_input = new HashSet<>();
        Set<String> pac_tem = new HashSet<>();
        intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        ResolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        for (ResolveInfo e : ResolveInfoList) {
            pac_launch.add(e.activityInfo.packageName);
            if ((e.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
                pac_tem.add(e.activityInfo.packageName);
            }
        }
        intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        for (ResolveInfo e : ResolveInfoList) {
            pac_home.add(e.activityInfo.packageName);
        }
        List<InputMethodInfo> inputMethodInfoList = ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).getInputMethodList();
        for (InputMethodInfo e : inputMethodInfoList) {
            pac_input.add(e.getPackageName());
        }
        if (pac_white == null) {
            pac_white = pac_tem;
        } else if (pac_white.retainAll(pac_launch)) {
            sharedPreferences.edit().putStringSet(PAC_WHITE, pac_white).apply();
        }
        if (pac_msg.retainAll(pac_launch)) {
            sharedPreferences.edit().putStringSet(PAC_MSG, pac_msg).apply();
        }
        pac_launch.removeAll(pac_white);
        pac_launch.removeAll(pac_home);
        pac_launch.removeAll(pac_input);
        pac_white.addAll(pac_home);
        pac_white.add("com.android.packageinstaller");
        pac_white.removeAll(pac_input);

    }

    /**
     * 用于设置的主要UI界面
     */
    private void mainUI() {
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        final ComponentName componentName = new ComponentName(MyAccessibilityService.this, MyDeviceAdminReceiver.class);
        final boolean b = metrics.heightPixels > metrics.widthPixels;
        final int width = b ? metrics.widthPixels : metrics.heightPixels;
        final int height = b ? metrics.heightPixels : metrics.widthPixels;
        final LayoutInflater inflater = LayoutInflater.from(MyAccessibilityService.this);
        final View view_main = inflater.inflate(R.layout.main_dialog, null);
        final AlertDialog dialog_main = new AlertDialog.Builder(MyAccessibilityService.this).setTitle(R.string.app_name).setIcon(R.drawable.a).setCancelable(false).setView(view_main).create();
        final Switch switch_skip_advertising = view_main.findViewById(R.id.skip_advertising);
        final Switch switch_volume_control = view_main.findViewById(R.id.volume_control);
        final Switch switch_record_message = view_main.findViewById(R.id.record_message);
        final Switch switch_record_clip = view_main.findViewById(R.id.record_clip);
        final Switch switch_screen_lightness = view_main.findViewById(R.id.screen_lightness);
        final Switch switch_screen_lock = view_main.findViewById(R.id.screen_lock);
        TextView bt_set = view_main.findViewById(R.id.set);
        TextView bt_look = view_main.findViewById(R.id.look);
        TextView bt_cancel = view_main.findViewById(R.id.cancel);
        TextView bt_sure = view_main.findViewById(R.id.sure);
        switch_skip_advertising.setChecked((asi.eventTypes & AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        switch_volume_control.setChecked((asi.flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) == AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
        switch_record_message.setChecked((asi.eventTypes & AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
        switch_record_clip.setChecked(record_clip);
        switch_screen_lightness.setChecked(control_lightness);
        switch_screen_lock.setChecked(control_lock && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || devicePolicyManager.isAdminActive(componentName)));

        switch_skip_advertising.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final View view = inflater.inflate(R.layout.skipdesc_parent, null);
                final AlertDialog dialog_adv = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).create();
                final LinearLayout parentView = view.findViewById(R.id.skip_desc);
                Button addButton = view.findViewById(R.id.add);
                Button chooseButton = view.findViewById(R.id.choose);
                final Button keyButton = view.findViewById(R.id.keyword);
                Set<Map.Entry<String, SkipButtonDescribe>> set = act_p.entrySet();
                for (Map.Entry<String, SkipButtonDescribe> e : set) {
                    final View childView = inflater.inflate(R.layout.skipdesc_child, null);
                    ImageView imageView = childView.findViewById(R.id.img);
                    TextView className = childView.findViewById(R.id.classname);
                    final EditText x = childView.findViewById(R.id.x);
                    final EditText y = childView.findViewById(R.id.y);
                    final EditText period = childView.findViewById(R.id.period);
                    final TextView modify = childView.findViewById(R.id.modify);
                    TextView delete = childView.findViewById(R.id.delete);
                    TextView sure = childView.findViewById(R.id.sure);
                    final SkipButtonDescribe value = e.getValue();
                    try {
                        imageView.setImageDrawable(packageManager.getApplicationIcon(value.packageName));
                    } catch (PackageManager.NameNotFoundException e1) {
                        imageView.setImageResource(R.drawable.u);
                        modify.setText("该应用未安装");
                    }
                    className.setText(value.className);
                    x.setText(String.valueOf(value.x));
                    y.setText(String.valueOf(value.y));
                    period.setText(String.valueOf(value.period));
                    delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            act_p.remove(value.className);
                            parentView.removeView(childView);
                        }
                    });
                    sure.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String sX = x.getText().toString();
                            String sY = y.getText().toString();
                            String sPeriod = period.getText().toString();
                            modify.setTextColor(0xffff0000);
                            if (sX.isEmpty() || sY.isEmpty() || sPeriod.isEmpty()) {
                                modify.setText("内容不能为空");
                                return;
                            } else if (Integer.valueOf(sX) < 0 || Integer.valueOf(sX) > metrics.widthPixels) {
                                modify.setText("X坐标超出屏幕寸");
                                return;
                            } else if (Integer.valueOf(sY) < 0 || Integer.valueOf(sY) > metrics.heightPixels) {
                                modify.setText("Y坐标超出屏幕寸");
                                return;
                            } else if (Integer.valueOf(sPeriod) < 100 || Integer.valueOf(sPeriod) > 1000) {
                                modify.setText("周期应为100~1000(ms)之间");
                                return;
                            } else {
                                value.x = Integer.valueOf(sX);
                                value.y = Integer.valueOf(sY);
                                value.period = Integer.valueOf(sPeriod);
                                modify.setText(new SimpleDateFormat("HH:mm:ss a", Locale.ENGLISH).format(new Date()) + "(修改成功)");
                                modify.setTextColor(0xff000000);
                            }
                        }
                    });
                    parentView.addView(childView);
                }
                chooseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View view = inflater.inflate(R.layout.view_select, null);
                        ListView listView = view.findViewById(R.id.listView);
                        final List<ResolveInfo> list = packageManager.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL);
                        final ArrayList<String> pac_name = new ArrayList<>();
                        final ArrayList<String> pac_label = new ArrayList<>();
                        final ArrayList<Drawable> drawables = new ArrayList<>();
                        for (ResolveInfo e : list) {
                            ApplicationInfo info = e.activityInfo.applicationInfo;
                            if (pac_home.contains(info.packageName) || pac_input.contains(info.packageName))
                                continue;
                            pac_name.add(info.packageName);
                            pac_label.add(packageManager.getApplicationLabel(info).toString());
                            drawables.add(info.loadIcon(packageManager));
                        }
                        BaseAdapter baseAdapter = new BaseAdapter() {
                            @Override
                            public int getCount() {
                                return pac_name.size();
                            }

                            @Override
                            public Object getItem(int position) {
                                return position;
                            }

                            @Override
                            public long getItemId(int position) {
                                return position;
                            }

                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                ViewHolder holder;
                                if (convertView == null) {
                                    convertView = inflater.inflate(R.layout.view_pac, null);
                                    holder = new ViewHolder(convertView);
                                    convertView.setTag(holder);
                                } else {
                                    holder = (ViewHolder) convertView.getTag();
                                }
                                holder.textView.setText(pac_label.get(position));
                                holder.imageView.setImageDrawable(drawables.get(position));
                                holder.checkBox.setChecked(pac_white.contains(pac_name.get(position)));
                                return convertView;
                            }
                        };
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                CheckBox c = ((ViewHolder) view.getTag()).checkBox;
                                String str = pac_name.get(position);
                                if (c.isChecked()) {
                                    pac_white.remove(str);
                                    pac_launch.add(str);
                                    c.setChecked(false);
                                } else {
                                    pac_white.add(str);
                                    pac_launch.remove(str);
                                    c.setChecked(true);
                                }
                            }
                        });
                        listView.setAdapter(baseAdapter);
                        AlertDialog dialog_pac = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                sharedPreferences.edit().putStringSet(PAC_WHITE, pac_white).apply();
                            }
                        }).create();

                        Window win = dialog_pac.getWindow();
                        win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                        win.setDimAmount(0);
                        dialog_pac.show();
                        WindowManager.LayoutParams params = win.getAttributes();
                        params.width = (width / 6) * 5;
                        win.setAttributes(params);
                        dialog_adv.dismiss();
                    }

                    class ViewHolder {
                        TextView textView;
                        ImageView imageView;
                        CheckBox checkBox;

                        public ViewHolder(View v) {
                            textView = v.findViewById(R.id.name);
                            imageView = v.findViewById(R.id.img);
                            checkBox = v.findViewById(R.id.check);
                        }
                    }
                });

                addButton.setOnClickListener(new View.OnClickListener() {
                    String className, packageName;
                    int pX, pY;

                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public void onClick(View v) {
                        if (target_xy != null && adv_view != null){
                            dialog_adv.dismiss();
                            return;
                        }
                        aParams = new WindowManager.LayoutParams();
                        bParams = new WindowManager.LayoutParams();
                        aParams.type = bParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
                        aParams.format = bParams.format = PixelFormat.TRANSPARENT;
                        aParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                        bParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                        aParams.gravity = bParams.gravity = Gravity.START | Gravity.TOP;
                        aParams.width = aParams.height = width / 4;
                        aParams.x = (metrics.widthPixels - aParams.width) / 2;
                        aParams.y = (metrics.heightPixels - aParams.height) / 2;
                        bParams.width = width;
                        bParams.height = height / 5;
                        bParams.x = (metrics.widthPixels - bParams.width) / 2;
                        bParams.y = metrics.heightPixels - bParams.height;
                        aParams.alpha = 0.5f;
                        bParams.alpha = 0.8f;
                        adv_view = inflater.inflate(R.layout.advertise_desc, null);
                        final TextView pacName = adv_view.findViewById(R.id.pacName);
                        final TextView actName = adv_view.findViewById(R.id.actName);
                        final TextView xyd = adv_view.findViewById(R.id.xyd);
                        Button saveButton = adv_view.findViewById(R.id.adv_add);
                        Button quitButton = adv_view.findViewById(R.id.quit);
                        target_xy = new ImageView(MyAccessibilityService.this);
                        target_xy.setImageResource(R.drawable.p);
                        adv_view.setOnTouchListener(new View.OnTouchListener() {
                            int x = 0, y = 0;

                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_DOWN:
                                        x = Math.round(event.getRawX());
                                        y = Math.round(event.getRawY());
                                        break;
                                    case MotionEvent.ACTION_MOVE:
                                        bParams.x = Math.round(bParams.x + (event.getRawX() - x));
                                        bParams.y = Math.round(bParams.y + (event.getRawY() - y));
                                        x = Math.round(event.getRawX());
                                        y = Math.round(event.getRawY());
                                        windowManager.updateViewLayout(adv_view, bParams);
                                        break;
                                }
                                return true;
                            }
                        });
                        target_xy.setOnTouchListener(new View.OnTouchListener() {
                            int x = 0, y = 0, width = aParams.width / 2, height = aParams.height / 2;

                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_DOWN:
                                        aParams.alpha = 0.9f;
                                        windowManager.updateViewLayout(target_xy, aParams);
                                        x = Math.round(event.getRawX());
                                        y = Math.round(event.getRawY());
                                        break;
                                    case MotionEvent.ACTION_MOVE:
                                        aParams.x = Math.round(aParams.x + (event.getRawX() - x));
                                        aParams.y = Math.round(aParams.y + (event.getRawY() - y));
                                        x = Math.round(event.getRawX());
                                        y = Math.round(event.getRawY());
                                        windowManager.updateViewLayout(target_xy, aParams);
                                        packageName = old_pac;
                                        className = cur_act;
                                        pX = aParams.x + width;
                                        pY = aParams.y + height;
                                        pacName.setText(packageName);
                                        actName.setText(className);
                                        xyd.setText("X坐标：" + pX + "    Y坐标：" + pY + "    点击周期：300ms(默认)");
                                        break;
                                    case MotionEvent.ACTION_UP:
                                        aParams.alpha = 0.5f;
                                        windowManager.updateViewLayout(target_xy, aParams);
                                        break;
                                }
                                return true;
                            }
                        });
                        quitButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String gJson = new Gson().toJson(act_p);
                                sharedPreferences.edit().putString(ACTIVITY_MAP, gJson).apply();
                                windowManager.removeViewImmediate(adv_view);
                                windowManager.removeViewImmediate(target_xy);
                                adv_view = null;
                                target_xy = null;
                                aParams = null;
                                bParams = null;
                            }
                        });
                        saveButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (packageName == null || className == null) return;
                                act_p.put(className, new SkipButtonDescribe(packageName, className, pX, pY, 300));
                                pacName.setText(packageName + " (以下数据已保存)");
                            }
                        });
                        windowManager.addView(adv_view, bParams);
                        windowManager.addView(target_xy, aParams);
                        dialog_adv.dismiss();
                    }
                });
                keyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View addKeyView = inflater.inflate(R.layout.add_keyword, null);
                        final LinearLayout layout = addKeyView.findViewById(R.id.keyList);
                        final EditText edit = addKeyView.findViewById(R.id.inputKet);
                        Button button = addKeyView.findViewById(R.id.addKey);
                        AlertDialog dialog_key = new AlertDialog.Builder(MyAccessibilityService.this).setView(addKeyView).setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                String gJson = new Gson().toJson(keyWordList);
                                sharedPreferences.edit().putString(KEY_WORD_LIST, gJson).apply();
                            }
                        }).create();
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final String input = edit.getText().toString();
                                if (!input.isEmpty()) {
                                    if (!keyWordList.contains(input)) {
                                        final View itemView = inflater.inflate(R.layout.keyword_item, null);
                                        final TextView text = itemView.findViewById(R.id.keyName);
                                        TextView rm = itemView.findViewById(R.id.remove);
                                        text.setText(input);
                                        layout.addView(itemView);
                                        keyWordList.add(input);
                                        rm.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                keyWordList.remove(text.getText().toString());
                                                layout.removeView(itemView);
                                            }
                                        });
                                    }
                                    edit.setText("");
                                }
                            }
                        });
                        for (String e : keyWordList) {
                            final View itemView = inflater.inflate(R.layout.keyword_item, null);
                            final TextView text = itemView.findViewById(R.id.keyName);
                            TextView rm = itemView.findViewById(R.id.remove);
                            text.setText(e);
                            layout.addView(itemView);
                            rm.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    keyWordList.remove(text.getText().toString());
                                    layout.removeView(itemView);
                                }
                            });
                        }
                        Window win = dialog_key.getWindow();
                        win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                        win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                        win.setDimAmount(0);
                        dialog_key.show();
                        WindowManager.LayoutParams params = win.getAttributes();
                        params.width = (width / 6) * 5;
                        win.setAttributes(params);
                        dialog_adv.dismiss();
                    }
                });
                dialog_adv.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        String gJson = new Gson().toJson(act_p);
                        sharedPreferences.edit().putString(ACTIVITY_MAP, gJson).apply();
                    }
                });
                Window win = dialog_adv.getWindow();
                win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                win.setDimAmount(0);
                dialog_adv.show();
                WindowManager.LayoutParams params = win.getAttributes();
                params.width = (width / 6) * 5;
                win.setAttributes(params);
                dialog_main.dismiss();
                return true;
            }
        });

        switch_volume_control.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                View view = inflater.inflate(R.layout.vibration_strength, null);
                SeekBar seekBar = view.findViewById(R.id.strength);
                seekBar.setProgress(vibration_strength);
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        vibration_strength = progress;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                AlertDialog dialog_vol = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        sharedPreferences.edit().putInt(VIBRATION_STRENGTH, vibration_strength).apply();
                    }
                }).create();
                Window win = dialog_vol.getWindow();
                win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                win.setDimAmount(0);
                dialog_vol.show();
                WindowManager.LayoutParams params = win.getAttributes();
                params.width = (width / 6) * 5;
                win.setAttributes(params);
                dialog_main.dismiss();
                return true;
            }
        });
        switch_record_message.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    final File file = new File(savePath + "/" + "NotificationMessageCache.txt");
                    final View view = inflater.inflate(R.layout.view_massage, null);
                    final AlertDialog dialog_message = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).create();
                    final EditText textView = view.findViewById(R.id.editText);
                    TextView but_choose = view.findViewById(R.id.choose);
                    TextView but_empty = view.findViewById(R.id.empty);
                    TextView but_cancel = view.findViewById(R.id.cancel);
                    TextView but_sure = view.findViewById(R.id.sure);
                    but_choose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            View view = inflater.inflate(R.layout.view_select, null);
                            ListView listView = view.findViewById(R.id.listView);
                            final List<ResolveInfo> list = packageManager.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL);
                            final ArrayList<String> pac_name = new ArrayList<>();
                            final ArrayList<String> pac_label = new ArrayList<>();
                            final ArrayList<Drawable> drawables = new ArrayList<>();
                            for (ResolveInfo e : list) {
                                ApplicationInfo info = e.activityInfo.applicationInfo;
                                pac_name.add(info.packageName);
                                pac_label.add(packageManager.getApplicationLabel(info).toString());
                                drawables.add(info.loadIcon(packageManager));
                            }
                            BaseAdapter baseAdapter = new BaseAdapter() {
                                @Override
                                public int getCount() {
                                    return pac_name.size();
                                }

                                @Override
                                public Object getItem(int position) {
                                    return position;
                                }

                                @Override
                                public long getItemId(int position) {
                                    return position;
                                }

                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {
                                    ViewHolder holder;
                                    if (convertView == null) {
                                        convertView = inflater.inflate(R.layout.view_pac, null);
                                        holder = new ViewHolder(convertView);
                                        convertView.setTag(holder);
                                    } else {
                                        holder = (ViewHolder) convertView.getTag();
                                    }
                                    holder.textView.setText(pac_label.get(position));
                                    holder.imageView.setImageDrawable(drawables.get(position));
                                    holder.checkBox.setChecked(pac_msg.contains(pac_name.get(position)));
                                    return convertView;
                                }
                            };
                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    CheckBox c = ((ViewHolder) view.getTag()).checkBox;
                                    if (c.isChecked()) {
                                        pac_msg.remove(pac_name.get(position));
                                        c.setChecked(false);
                                    } else {
                                        pac_msg.add(pac_name.get(position));
                                        c.setChecked(true);
                                    }
                                }
                            });
                            listView.setAdapter(baseAdapter);
                            AlertDialog dialog_pac = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    sharedPreferences.edit().putStringSet(PAC_MSG, pac_msg).apply();
                                }
                            }).create();

                            Window win = dialog_pac.getWindow();
                            win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                            win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                            win.setDimAmount(0);
                            dialog_pac.show();
                            WindowManager.LayoutParams params = win.getAttributes();
                            params.width = (width / 6) * 5;
                            win.setAttributes(params);
                            dialog_message.dismiss();
                        }

                        class ViewHolder {
                            TextView textView;
                            ImageView imageView;
                            CheckBox checkBox;

                            public ViewHolder(View v) {
                                textView = v.findViewById(R.id.name);
                                imageView = v.findViewById(R.id.img);
                                checkBox = v.findViewById(R.id.check);
                            }
                        }
                    });
                    but_empty.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            textView.setText("");
                        }
                    });
                    but_cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog_message.dismiss();
                        }
                    });
                    but_sure.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                FileWriter writer = new FileWriter(file, false);
                                writer.write(textView.getText().toString());
                                writer.close();
                                dialog_message.dismiss();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    if (file.exists()) {
                        StringBuilder builder = new StringBuilder();
                        Scanner scanner = new Scanner(file);
                        while (scanner.hasNextLine()) {
                            builder.append(scanner.nextLine() + "\n");
                        }
                        scanner.close();
                        textView.setText(builder.toString());
                        textView.setSelection(builder.length());
                    } else {
                        textView.setHint("当前文件内容为空，如果还没有选择要记录其通知的应用，请点击下方‘选择应用’进行勾选。");
                    }
                    Window win = dialog_message.getWindow();
                    win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                    win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                    win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    win.setDimAmount(0);
                    dialog_message.show();
                    WindowManager.LayoutParams params = win.getAttributes();
                    params.width = width;
                    win.setAttributes(params);
                    dialog_main.dismiss();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        switch_record_clip.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    final File file = new File(savePath + "/" + "ClipContentCache.txt");
                    final View view = inflater.inflate(R.layout.view_clip, null);
                    final AlertDialog dialog_clip = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).create();
                    final EditText textView = view.findViewById(R.id.editText);
                    TextView but_empty = view.findViewById(R.id.empty);
                    TextView but_cancel = view.findViewById(R.id.cancel);
                    TextView but_sure = view.findViewById(R.id.sure);
                    but_empty.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            textView.setText("");
                        }
                    });
                    but_cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog_clip.dismiss();
                        }
                    });
                    but_sure.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                FileWriter writer = new FileWriter(file, false);
                                writer.write(textView.getText().toString());
                                writer.close();
                                dialog_clip.dismiss();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    if (file.exists()) {
                        StringBuilder builder = new StringBuilder();
                        Scanner scanner = new Scanner(file);
                        while (scanner.hasNextLine()) {
                            builder.append(scanner.nextLine() + "\n");
                        }
                        scanner.close();
                        textView.setText(builder.toString());
                        textView.setSelection(builder.length());
                    } else {
                        textView.setHint("当前文件内容为空");
                    }
                    Window win = dialog_clip.getWindow();
                    win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                    win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                    win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    win.setDimAmount(0);
                    dialog_clip.show();
                    WindowManager.LayoutParams params = win.getAttributes();
                    params.width = width;
                    win.setAttributes(params);
                    dialog_main.dismiss();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        switch_screen_lightness.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                screenLightness.showControlDialog();
                dialog_main.dismiss();
                return true;
            }
        });
        switch_screen_lock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !devicePolicyManager.isAdminActive(componentName) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)) {
                    Intent intent = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    control_lock = false;
                    dialog_main.dismiss();
                }
            }
        });

        bt_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                dialog_main.dismiss();
            }
        });
        bt_look.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyAccessibilityService.this, HelpActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                dialog_main.dismiss();
            }
        });
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_main.dismiss();
            }
        });
        bt_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (switch_skip_advertising.isChecked()) {
                    asi.eventTypes |= AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    editor.putInt(EVENT_TYPES, asi.eventTypes | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply();

                } else {
                    asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    editor.putInt(EVENT_TYPES, asi.eventTypes & (~AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)).apply();
                }
                if (switch_volume_control.isChecked()) {
                    asi.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
                    editor.putInt(FLAGS, asi.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS).apply();
                } else {
                    asi.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
                    editor.putInt(FLAGS, asi.flags & (~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)).apply();
                }
                if (switch_record_message.isChecked()) {
                    asi.eventTypes |= AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
                    editor.putInt(EVENT_TYPES, asi.eventTypes | AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED).apply();

                } else {
                    asi.eventTypes &= ~AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
                    editor.putInt(EVENT_TYPES, asi.eventTypes & (~AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)).apply();
                }
                if (switch_record_clip.isChecked()) {
                    if (!record_clip) {
                        clipboardManager.addPrimaryClipChangedListener(primaryClipChangedListener);
                        record_clip = true;
                        editor.putBoolean(RECORD_CLIP, true).apply();
                    }
                } else {
                    if (record_clip) {
                        clipboardManager.removePrimaryClipChangedListener(primaryClipChangedListener);
                        record_clip = false;
                        editor.putBoolean(RECORD_CLIP, false).apply();
                    }
                }
                if (switch_screen_lightness.isChecked()) {
                    if (!control_lightness) {
                        screenLightness.showFloat();
                        control_lightness = true;
                        editor.putBoolean(CONTROL_LIGHTNESS, true).apply();
                    }
                } else {
                    if (control_lightness) {
                        screenLightness.dismiss();
                        control_lightness = false;
                        editor.putBoolean(CONTROL_LIGHTNESS, false).apply();
                    }
                }
                if (switch_screen_lock.isChecked()) {
                    if (!control_lock) {
                        screenLock.showLockFloat();
                        control_lock = true;
                        editor.putBoolean(CONTROL_LOCK, true).apply();
                    }
                } else {
                    if (control_lock) {
                        screenLock.dismiss();
                        control_lock = false;
                        editor.putBoolean(CONTROL_LOCK, false).apply();
                    }
                }
                setServiceInfo(asi);
                dialog_main.dismiss();
            }
        });
        Window win = dialog_main.getWindow();
        win.setBackgroundDrawableResource(R.drawable.dialogbackground);
        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        win.setDimAmount(0);
        dialog_main.show();
        WindowManager.LayoutParams params = win.getAttributes();
        params.width = (width / 6) * 5;
        win.setAttributes(params);
    }
}
