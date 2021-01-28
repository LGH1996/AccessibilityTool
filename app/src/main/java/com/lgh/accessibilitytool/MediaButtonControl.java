package com.lgh.accessibilitytool;

import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;


public class MediaButtonControl {

    private AudioManager audioManager;

    public MediaButtonControl(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void sendMediaButton(int keycode) {
        KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keycode);
        audioManager.dispatchMediaKeyEvent(downEvent);
        KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keycode);
        audioManager.dispatchMediaKeyEvent(upEvent);
    }

}
