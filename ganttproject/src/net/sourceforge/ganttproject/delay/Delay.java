package net.sourceforge.ganttproject.delay;

import net.sourceforge.ganttproject.task.TaskInfo;

/**
 * This class represents very basicaly delays. There are three type of delays : -
 * No delay - Normal delay - Critical delay (when the related task is critical)
 * 
 * @author bbaranne
 */
public class Delay implements TaskInfo {
    public static final int NONE = -1;

    public static final int NORMAL = 0;

    public static final int CRITICAL = 1;

    private int myType = NONE;

    private Delay(int type) {
        myType = type;
    }

    public int getType() {
        return myType;
    }

    public void setType(int type) {
        myType = type;
    }

    public static Delay getDelay(int type) {
        return new Delay(type);
    }
}
