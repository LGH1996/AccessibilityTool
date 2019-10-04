package com.lgh.accessibilitytool;

public class SkipButtonDescribe {
    public String packageName;
    public String className;
    public int x;
    public int y;
    public int delay;
    public int period;
    public int number;

    public SkipButtonDescribe(String packageName, String className, int x, int y,int delay, int period, int number) {
        this.packageName = packageName;
        this.className = className;
        this.x = x;
        this.y = y;
        this.delay = delay;
        this.period = period;
        this.number = number;
    }
}
