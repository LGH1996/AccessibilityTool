package com.lgh.accessibilitytool;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MyAccessibilityService extends AccessibilityService {

    static String TAG = "MyAccessibilityService";
    static String CONTROL_LIGHTNESS = "control_lightness";
    static String CONTROL_LOCK = "control_lock";
    static String EVENT_TYPES = "event_type";
    static String FLAGS = "flags";
    static String PAC_MSG = "pac_msg";
    static String VIBRATION_STRENGTH = "vibration_strength";
    public static Handler handler;
    boolean double_press;
    boolean is_release_up, is_release_down;
    boolean control_lightness, control_lock, is_state_change;
    long star_up, star_down;
    int win_state_count, create_num, connect_num, vibration_strength;
    SharedPreferences sharedPreferences;
    ScheduledFuture future;
    ScheduledExecutorService executorService;
    AudioManager audioManager;
    PackageManager packageManager;
    Vibrator vibrator;
    Set<String> pac_msg, pac_system;
    ArrayList<String> pac_home, pac_input;
    AccessibilityServiceInfo asi;
    String old_pac;
    WindowManager windowManager;
    DevicePolicyManager devicePolicyManager;
    ScreenLightness screenLightness;
    MediaButtonControl mediaButtonControl;
    ScreenLock screenLock;

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
            is_release_up = true;
            is_release_down = true;
            is_state_change = false;
            double_press = false;
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            packageManager = getPackageManager();
            executorService = Executors.newSingleThreadScheduledExecutor();
            mediaButtonControl = new MediaButtonControl(this);
            screenLightness = new ScreenLightness(this);
            screenLock = new ScreenLock(this);
            old_pac = getPackageName();
            asi = getServiceInfo();
            control_lightness = sharedPreferences.getBoolean(CONTROL_LIGHTNESS, false);
            vibration_strength = sharedPreferences.getInt(VIBRATION_STRENGTH, 50);
            control_lock = sharedPreferences.getBoolean(CONTROL_LOCK, true) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || devicePolicyManager.isAdminActive(new ComponentName(this, MyDeviceAdminReceiver.class)));
            asi.eventTypes = sharedPreferences.getInt(EVENT_TYPES, AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            asi.flags = sharedPreferences.getInt(FLAGS, asi.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
            setServiceInfo(asi);
            pac_msg = sharedPreferences.getStringSet(PAC_MSG, new HashSet<String>());
            pac_home = new ArrayList<>();
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> ResolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
            for (ResolveInfo e : ResolveInfoList) {
                pac_home.add(e.activityInfo.packageName);
            }
            pac_input = new ArrayList<>();
            List<InputMethodInfo> inputMethodInfoList = ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).getInputMethodList();
            for (InputMethodInfo e : inputMethodInfoList) {
                pac_input.add(e.getPackageName());
            }
            pac_system = new HashSet<>();
            List<ApplicationInfo> packageInfoList = packageManager.getInstalledApplications(0);
            for (ApplicationInfo e : packageInfoList) {
                if ((e.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
                    pac_system.add(e.packageName);
                }
            }
            pac_system.addAll(pac_input);
            pac_system.removeAll(pac_home);
            if (control_lightness) screenLightness.showFloat();
            if (control_lock) screenLock.showLockFloat();
            handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case 0x00:
                            DisplayMetrics metrics = new DisplayMetrics();
                            windowManager.getDefaultDisplay().getMetrics(metrics);
                            final ComponentName componentName = new ComponentName(MyAccessibilityService.this, MyDeviceAdminReceiver.class);
                            final int width = (metrics.widthPixels / 6) * 5;
                            final int height = metrics.heightPixels;
                            final LayoutInflater inflater = LayoutInflater.from(MyAccessibilityService.this);
                            final View view_main = inflater.inflate(R.layout.main_dialog, null);
                            final AlertDialog dialog_main = new AlertDialog.Builder(MyAccessibilityService.this).setTitle(R.string.app_name).setIcon(R.drawable.a).setCancelable(false).setView(view_main).create();
                            final Switch switch_skip_advertising = view_main.findViewById(R.id.skip_advertising);
                            final Switch switch_volume_control = view_main.findViewById(R.id.volume_control);
                            final Switch switch_record_message = view_main.findViewById(R.id.record_message);
                            final Switch switch_screen_lightness = view_main.findViewById(R.id.screen_lightness);
                            final Switch switch_screen_lock = view_main.findViewById(R.id.screen_lock);
                            TextView bt_set = view_main.findViewById(R.id.set);
                            TextView bt_look = view_main.findViewById(R.id.look);
                            TextView bt_cancel = view_main.findViewById(R.id.cancel);
                            TextView bt_sure = view_main.findViewById(R.id.sure);
                            switch_skip_advertising.setChecked((asi.eventTypes & AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                            switch_volume_control.setChecked((asi.flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) == AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
                            switch_record_message.setChecked((asi.eventTypes & AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
                            switch_screen_lightness.setChecked(control_lightness);
                            switch_screen_lock.setChecked(control_lock && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || devicePolicyManager.isAdminActive(componentName)));
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
                                    AlertDialog dialog = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            sharedPreferences.edit().putInt(VIBRATION_STRENGTH, vibration_strength).apply();
                                        }
                                    }).create();
                                    Window win = dialog.getWindow();
                                    win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                                    win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                                    win.setDimAmount(0);
                                    dialog.show();
                                    WindowManager.LayoutParams params = win.getAttributes();
                                    params.width = width;
                                    win.setAttributes(params);
                                    dialog_main.dismiss();
                                    return true;
                                }
                            });
                            switch_record_message.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    View view = inflater.inflate(R.layout.view_select, null);
                                    ListView listView = view.findViewById(R.id.listview);
                                    final List<ApplicationInfo> list = packageManager.getInstalledApplications(0);
                                    final ArrayList<String> pac_name = new ArrayList<>();
                                    final ArrayList<String> pac_label = new ArrayList<>();
                                    final ArrayList<Drawable> drawables = new ArrayList<>();
                                    for (ApplicationInfo e : list) {
                                        if ((e.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) {
                                            pac_name.add(e.packageName);
                                            pac_label.add(packageManager.getApplicationLabel(e).toString());
                                            drawables.add(e.loadIcon(packageManager));
                                        }
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
                                                holder = new ViewHolder();
                                                convertView = inflater.inflate(R.layout.view_list, null);
                                                holder.textView = convertView.findViewById(R.id.name);
                                                holder.imageView = convertView.findViewById(R.id.img);
                                                holder.checkBox = convertView.findViewById(R.id.check);
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
                                    AlertDialog dialog = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            sharedPreferences.edit().putStringSet(PAC_MSG, pac_msg).apply();
                                        }
                                    }).create();

                                    Window win = dialog.getWindow();
                                    win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                                    win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                                    win.setDimAmount(0);
                                    dialog.show();
                                    WindowManager.LayoutParams params = win.getAttributes();
                                    params.width = width;
                                    win.setAttributes(params);
                                    dialog_main.dismiss();
                                    return true;
                                }

                                class ViewHolder {
                                    TextView textView;
                                    ImageView imageView;
                                    CheckBox checkBox;
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
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    dialog_main.dismiss();
                                }
                            });
                            bt_look.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        File file = new File(getExternalCacheDir().getAbsolutePath() + "/" + "NotificationMessageCache.txt");
                                        if (!file.exists()) {
                                            Toast.makeText(MyAccessibilityService.this, "当前文件记录为空", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        final View view = inflater.inflate(R.layout.view_massage, null);
                                        final AlertDialog dialog = new AlertDialog.Builder(MyAccessibilityService.this).setView(view).create();
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
                                                dialog.dismiss();
                                            }
                                        });
                                        but_sure.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                try {
                                                    FileWriter writer = new FileWriter(getExternalCacheDir().getAbsolutePath() + "/" + "NotificationMessageCache.txt", false);
                                                    writer.write(textView.getText().toString());
                                                    writer.close();
                                                    dialog.dismiss();
                                                } catch (Throwable e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                        Scanner scanner = new Scanner(file);
                                        StringBuilder builder = new StringBuilder();
                                        while (scanner.hasNextLine()) {
                                            builder.append(scanner.nextLine() + "\n");
                                        }
                                        scanner.close();
                                        textView.setText(builder.toString());
                                        textView.setSelection(builder.length());
                                        Window win = dialog.getWindow();
                                        win.setBackgroundDrawableResource(R.drawable.dialogbackground);
                                        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                                        win.setDimAmount(0);
                                        dialog.show();
                                        WindowManager.LayoutParams params = win.getAttributes();
                                        params.width = width;
                                        params.height = (height / 6) * 5;
                                        win.setAttributes(params);
                                        dialog_main.dismiss();
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }

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
                                    if (switch_screen_lightness.isChecked()) {
                                        screenLightness.showFloat();
                                        control_lightness = true;
                                        editor.putBoolean(CONTROL_LIGHTNESS, true).apply();
                                    } else {
                                        screenLightness.dismiss();
                                        control_lightness = false;
                                        editor.putBoolean(CONTROL_LIGHTNESS, false).apply();
                                    }
                                    if (switch_screen_lock.isChecked()) {
                                        screenLock.showLockFloat();
                                        control_lock = true;
                                        editor.putBoolean(CONTROL_LOCK, true).apply();
                                    } else {
                                        screenLock.dismiss();
                                        control_lock = false;
                                        editor.putBoolean(CONTROL_LOCK, false).apply();
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
                            params.width = width;
                            win.setAttributes(params);
                            break;
                        case 0x01:
                            performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
                            break;
                        case 0x02:
                            performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                            break;
                    }
                    return true;
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        Log.i(TAG,AccessibilityEvent.eventTypeToString(event.getEventType())+"|"+event.getPackageName()+"|"+event.getClassName()+"|"+win_state_count);
        try {
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    String str = event.getPackageName().toString();
                    if (!(str.equals(old_pac) || event.getClassName().toString().startsWith("android.widget.") || pac_system.contains(str))) {
                        old_pac = str;
                        if (pac_home.contains(str)) break;
                        asi.eventTypes |= AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                        setServiceInfo(asi);
                        is_state_change = true;
                        win_state_count = 0;
                        findSkipButton(event);
                        if (future != null)
                            future.cancel(true);
                        future = executorService.schedule(new Runnable() {
                            @Override
                            public void run() {
                                asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                                setServiceInfo(asi);
                                is_state_change = false;
                            }
                        }, 8000, TimeUnit.MILLISECONDS);
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                    if (is_state_change && !event.getPackageName().equals("com.android.systemui")) {
                        findSkipButton(event);
                    }
                    break;
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    if (event.getParcelableData() instanceof Notification && pac_msg.contains(event.getPackageName())) {
                        List<CharSequence> list_msg = event.getText();
                        StringBuilder builder = new StringBuilder();
                        for (CharSequence s : list_msg)
                            builder.append("[" + s.toString().replaceAll("\\s", "") + "]");
                        builder.append("\n");
                        FileWriter writer = new FileWriter(getExternalCacheDir().getAbsolutePath() + "/" + "NotificationMessageCache.txt", true);
                        writer.append(builder.toString());
                        writer.close();
                    }
                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
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
                            if (future != null)
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
                            if (future != null)
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
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 列举出所
     * 有的节点
     */
    private void findnode(AccessibilityNodeInfo root, List<AccessibilityNodeInfo> list) {
        for (int n = 0; n < root.getChildCount(); n++) {
            AccessibilityNodeInfo nod = root.getChild(n);
            if (nod != null) {
                if (nod.getChildCount() == 0) {
                    list.add(nod);
                } else {
                    findnode(nod, list);
                }
            }
        }
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

    private void findSkipButton(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = win_state_count <= 2 ? getRootInActiveWindow() : event.getSource();
        if (nodeInfo == null) return;
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("跳过");
        for (AccessibilityNodeInfo e : list) {
            if (!e.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                if (!e.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Rect rect = new Rect();
                    e.getBoundsInScreen(rect);
                    click(rect.centerX(), rect.centerY(), 0, 10);
                }
            }
        }
        if (!list.isEmpty() || win_state_count >= 20) {
            asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            setServiceInfo(asi);
            is_state_change = false;
            if (future != null)
                future.cancel(true);
        }
        win_state_count++;
    }
}
