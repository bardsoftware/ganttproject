/*
 * Created on 09.11.2004
 */
package net.sourceforge.ganttproject.time;

/**
 * @author bard
 */
public class TimeUnitPair {
    private final TimeUnit myBottomTimeUnit;

    private final TimeUnit myTopTimeUnit;

    private final TimeUnitStack myTimeUnitStack;

    protected TimeUnitPair(TimeUnit topUnit, TimeUnit bottomUnit,
            TimeUnitStack timeUnitStack) {
        myTopTimeUnit = topUnit;
        myBottomTimeUnit = bottomUnit;
        myTimeUnitStack = timeUnitStack;
    }

    public TimeUnit getTopTimeUnit() {
        return myTopTimeUnit;
    }

    public TimeUnit getBottomTimeUnit() {
        return myBottomTimeUnit;
    }

    public TimeUnitStack getTimeUnitStack() {
        return myTimeUnitStack;
    }
}
