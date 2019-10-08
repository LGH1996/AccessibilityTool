package com.lgh.accessibilitytool;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

public class MyMethodClass {
    public interface DrawAllNode{
        void draw(ArrayList<AccessibilityNodeInfo> roots);
    }
}
