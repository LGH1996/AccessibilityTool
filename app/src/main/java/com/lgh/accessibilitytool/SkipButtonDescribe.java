package com.lgh.accessibilitytool;

public class SkipButtonDescribe {
    public String packageName;
    public String className;
    public int x;
    public int y;
    public int period;

    public SkipButtonDescribe(String packageName, String className, int x, int y, int period) {
        this.packageName = packageName;
        this.className = className;
        this.x = x;
        this.y = y;
        this.period = period;
    }
}
