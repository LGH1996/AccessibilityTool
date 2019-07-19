package com.lgh.accessibilitytool;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.AlertDialog;
import android.app.Notification;
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
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MyAccessibilityService extends AccessibilityService {

    //    static String TAG = "MyAccessibilityService";
    public static Handler handler;
    boolean double_press;
    boolean isrelease_up, isrelease_down, winchg_occur;
    long star_up, star_down;
    int winstatus_count, create_num, connect_num;
    SharedPreferences sharedPreferences;
    ScheduledFuture future;
    ScheduledExecutorService executorService;
    AudioManager audioManager;
    Vibrator vibrator;
    ArrayList<String> pac_clk;
    Set<String> pac_msg;
    AccessibilityServiceInfo asi;

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
            isrelease_up = true;
            isrelease_down = true;
            winchg_occur = false;
            double_press = false;
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
            executorService = Executors.newSingleThreadScheduledExecutor();
            asi = getServiceInfo();
            asi.eventTypes = sharedPreferences.getInt("eventTypes", AccessibilityEvent.TYPE_VIEW_CLICKED | AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
            asi.flags = sharedPreferences.getInt("flags", asi.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
            setServiceInfo(asi);
            pac_clk = new ArrayList<>();
            pac_clk.add("com.android.systemui");
            pac_clk.add("com.android.packageinstaller");
            pac_clk.add("android");
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo rsf = getPackageManager().resolveActivity(intent, 0);
            if (rsf != null) pac_clk.add(rsf.activityInfo.packageName);
            pac_msg = sharedPreferences.getStringSet("pac_msg", new HashSet<String>());
            handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case 0x00:
                            final Set<String> pac_tem = new HashSet<>(pac_msg);
                            final LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                            View view_1 = inflater.inflate(R.layout.floatlayout, null);
                            final Switch switch_skip = view_1.findViewById(R.id.skip);
                            final Switch switch_control = view_1.findViewById(R.id.control);
                            final Switch switch_record = view_1.findViewById(R.id.record);
                            switch_skip.setChecked((asi.eventTypes & AccessibilityEvent.TYPE_VIEW_CLICKED) == AccessibilityEvent.TYPE_VIEW_CLICKED);
                            switch_control.setChecked((asi.flags & AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) == AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS);
                            switch_record.setChecked((asi.eventTypes & AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
                            switch_record.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    View view_2 = inflater.inflate(R.layout.activity_select, null);
                                    ListView listView = view_2.findViewById(R.id.listview);
                                    final PackageManager packageManager = getPackageManager();
                                    final List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                                    ListIterator<ApplicationInfo> iterator = list.listIterator();
                                    final ArrayList<String> pac_name = new ArrayList<>();
                                    final ArrayList<String> pac_label = new ArrayList<>();
                                    final ArrayList<Drawable> drawables = new ArrayList<>();
                                    while (iterator.hasNext()) {
                                        ApplicationInfo next = iterator.next();
                                        if ((next.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
                                            iterator.remove();
                                        } else {
                                            pac_name.add(next.packageName);
                                            pac_label.add(packageManager.getApplicationLabel(next).toString());
                                            drawables.add(next.loadIcon(packageManager));
                                        }
                                    }
                                    BaseAdapter baseAdapter = new BaseAdapter() {
                                        @Override
                                        public int getCount() {
                                            return list.size();
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
                                            holder.checkBox.setChecked(pac_tem.contains(pac_name.get(position)));
                                            return convertView;
                                        }
                                    };
                                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            CheckBox c = ((ViewHolder) view.getTag()).checkBox;
                                            if (c.isChecked()) {
                                                pac_tem.remove(pac_name.get(position));
                                                c.setChecked(false);
                                            } else {
                                                pac_tem.add(pac_name.get(position));
                                                c.setChecked(true);
                                            }
                                        }
                                    });
                                    listView.setAdapter(baseAdapter);
                                    AlertDialog dialog_2 = new AlertDialog.Builder(MyAccessibilityService.this).setView(view_2).create();
                                    Window win_2 = dialog_2.getWindow();
                                    win_2.setBackgroundDrawableResource(R.drawable.dialogbackground);
                                    win_2.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                                    dialog_2.show();
                                    return true;
                                }

                                class ViewHolder {
                                    TextView textView;
                                    ImageView imageView;
                                    CheckBox checkBox;
                                }
                            });

                            AlertDialog dialog_1 = new AlertDialog.Builder(MyAccessibilityService.this).setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (switch_skip.isChecked()) {
                                        asi.eventTypes |= AccessibilityEvent.TYPE_VIEW_CLICKED;
                                        sharedPreferences.edit().putInt("eventTypes", asi.eventTypes | AccessibilityEvent.TYPE_VIEW_CLICKED).commit();

                                    } else {
                                        asi.eventTypes &= ~AccessibilityEvent.TYPE_VIEW_CLICKED;
                                        sharedPreferences.edit().putInt("eventTypes", asi.eventTypes & (~AccessibilityEvent.TYPE_VIEW_CLICKED)).commit();
                                    }
                                    if (switch_control.isChecked()) {
                                        asi.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
                                        sharedPreferences.edit().putInt("flags", asi.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS).commit();
                                    } else {
                                        asi.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
                                        sharedPreferences.edit().putInt("flags", asi.flags & (~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)).commit();
                                    }
                                    if (switch_record.isChecked()) {
                                        asi.eventTypes |= AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
                                        sharedPreferences.edit().putInt("eventTypes", asi.eventTypes | AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED).commit();

                                    } else {
                                        asi.eventTypes &= ~AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
                                        sharedPreferences.edit().putInt("eventTypes", asi.eventTypes & (~AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)).commit();
                                    }
                                    setServiceInfo(asi);
                                    pac_msg = pac_tem;
                                    sharedPreferences.edit().putStringSet("pac_msg", pac_tem).commit();
                                }
                            }).setNeutralButton("设置", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }
                            }).setTitle(R.string.app_name).setIcon(R.drawable.a).setCancelable(false).setView(view_1).create();
                            Window win_1 = dialog_1.getWindow();
                            win_1.setBackgroundDrawableResource(R.drawable.dialogbackground);
                            win_1.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
                            dialog_1.show();
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
//        Log.i(TAG,AccessibilityEvent.eventTypeToString(event.getEventType()));
        try {
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    if (pac_clk.contains(event.getPackageName().toString())) {
                        asi.eventTypes |= (AccessibilityEvent.TYPE_WINDOWS_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
                        setServiceInfo(asi);
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                    winchg_occur = true;
                    winstatus_count = 0;
                    break;
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                    if (winchg_occur) {
                        AccessibilityNodeInfo info = getRootInActiveWindow();
                        if (info == null) break;
                        List<AccessibilityNodeInfo> list = info.findAccessibilityNodeInfosByText("跳过");
                        if (!list.isEmpty()) {
                            for (AccessibilityNodeInfo e : list) {
                                if (!e.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    if (!e.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                        Rect rect = new Rect();
                                        e.getBoundsInScreen(rect);
                                        click(rect.centerX(), rect.centerY(), 0, 10);
                                    }
                                }
                            }
                        }
                        if (!list.isEmpty() || winstatus_count >= 8) {
                            winchg_occur = false;
                            asi.eventTypes &= ~(AccessibilityEvent.TYPE_WINDOWS_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
                            setServiceInfo(asi);
                        }
                        winstatus_count++;
                    }
                    break;
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    if (event.getParcelableData() instanceof Notification && pac_msg.contains(event.getPackageName())) {
                        List<CharSequence> list_msg = event.getText();
                        StringBuilder builder = new StringBuilder();
                        for (CharSequence s : list_msg)
                            builder.append("【" + s.toString().replaceAll("\\s", "") + "】");
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
                            isrelease_up = false;
                            double_press = false;
                            if (isrelease_down) {
                                future = executorService.schedule(new Runnable() {
                                    @Override
                                    public void run() {
//                                        Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_UP -> THREAD");
                                        if (!isrelease_down) {
                                            play_pause_Music();
                                            vibrator.vibrate(8);
                                        } else if (!isrelease_up && audioManager.isMusicActive()) {
                                            nextMusic();
                                            vibrator.vibrate(8);
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
                            isrelease_up = true;
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
                            isrelease_down = false;
                            double_press = false;
                            if (isrelease_up) {
                                future = executorService.schedule(new Runnable() {
                                    @Override
                                    public void run() {
//                                        Log.i(TAG,"KeyEvent.KEYCODE_VOLUME_DOWN -> THREAD");
                                        if (!isrelease_up) {
                                            play_pause_Music();
                                            vibrator.vibrate(8);
                                        } else if (!isrelease_down && audioManager.isMusicActive()) {
                                            previousMusic();
                                            vibrator.vibrate(8);
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
                            isrelease_down = true;
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

    /**
     * 播放
     * 暂停
     */
    public void play_pause_Music() {
        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent downEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 1);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        sendOrderedBroadcast(downIntent, null);
        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent upEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 1);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        sendOrderedBroadcast(upIntent, null);
    }

    /**
     * 上一曲
     */
    public void previousMusic() {
        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent downEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 1);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        sendOrderedBroadcast(downIntent, null);
        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent upEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 1);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        sendOrderedBroadcast(upIntent, null);
    }

    /**
     * 下一曲
     */
    public void nextMusic() {
        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent downEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 1);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        sendOrderedBroadcast(downIntent, null);
        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent upEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 1);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        sendOrderedBroadcast(upIntent, null);
    }

}
