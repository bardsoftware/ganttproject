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

    /** Used scale for this TimeUnit */
    private final double myScale;

    protected TimeUnitPair(TimeUnit topUnit, TimeUnit bottomUnit,
            TimeUnitStack timeUnitStack, double scale) {
        myTopTimeUnit = topUnit;
        myBottomTimeUnit = bottomUnit;
        myTimeUnitStack = timeUnitStack;
        myScale = scale;
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

    /** @return the scale for this TimeUnit */
    public double getScale() {
        return myScale;
    }
}
