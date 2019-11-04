package com.lgh.accessibilitytool;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.KeyEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediaButtonControl {

    public boolean support_SysMusic;
    private Context context;
    private Set<String> music_set;

    public MediaButtonControl(Context context) {
        this.context = context;
        support_SysMusic = false;
        updateMusicSet();
    }

    public void updateMusicSet() {
        music_set = new HashSet<>();
        List<ResolveInfo> list = context.getPackageManager().queryBroadcastReceivers(new Intent(Intent.ACTION_MEDIA_BUTTON), PackageManager.MATCH_ALL);
        for (ResolveInfo e : list) {
            ApplicationInfo applicationInfo = e.activityInfo.applicationInfo;
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) {
                music_set.add(applicationInfo.packageName);
            }
        }
    }

    private void sendMediaButton(int keycode) {
        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keycode);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keycode);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        if (support_SysMusic) {
            context.sendOrderedBroadcast(downIntent, null);
            context.sendOrderedBroadcast(upIntent, null);
            return;
        }
        for (String e : music_set) {
            downIntent.setPackage(e);
            context.sendOrderedBroadcast(downIntent, null);
            upIntent.setPackage(e);
            context.sendOrderedBroadcast(upIntent, null);
        }
    }

    /**
     * 播放
     * 暂停
     */
    public void play_pause_Music() {
        sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }

    /**
     * 上一曲
     */
    public void previousMusic() {
        sendMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    /**
     * 下一曲
     */
    public void nextMusic() {
        sendMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT);
    }
}
