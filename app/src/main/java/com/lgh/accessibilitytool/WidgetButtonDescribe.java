package com.lgh.accessibilitytool;

import android.graphics.Rect;

public class WidgetButtonDescribe {
    public String packageName, activityName, className, idName, describe, text;
    public Rect bonus;
    public WidgetButtonDescribe(String packageName, String activityName,String className,String idName,String describe,String text,Rect bonus){
        this.packageName = packageName;
        this.activityName = activityName;
        this.className = className;
        this.idName = idName;
        this.describe = describe;
        this.text = text;
        this.bonus = bonus;
    }

    public WidgetButtonDescribe(WidgetButtonDescribe widgetDescribe){
        this.packageName = widgetDescribe.packageName;
        this.activityName = widgetDescribe.activityName;
        this.className = widgetDescribe.className;
        this.idName = widgetDescribe.idName;
        this.describe = widgetDescribe.describe;
        this.text = widgetDescribe.text;
        this.bonus = new Rect(widgetDescribe.bonus);

    }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (this == obj) return true;
            if (!(obj instanceof WidgetButtonDescribe)) return false;
            WidgetButtonDescribe widget = (WidgetButtonDescribe) obj;
            return bonus.equals(widget.bonus);
        }

    @Override
    public int hashCode() {
        return bonus.hashCode();
    }
}
