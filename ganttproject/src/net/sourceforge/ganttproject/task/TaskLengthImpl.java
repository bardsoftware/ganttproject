package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 31.01.2004
 */
public class TaskLengthImpl implements TaskLength {
    private final TimeUnit myUnit;

    private float myCount;

    public TaskLengthImpl(TimeUnit unit, long count) {
        myUnit = unit;
        myCount = count;
    }

    /**
     * @param unit
     * @param length
     */
    public TaskLengthImpl(TimeUnit unit, float length) {
        myUnit = unit;
        myCount = length;
    }

    public float getValue() {
        return myCount;
    }

    public long getLength() {
        return (long) myCount;
    }

    public TimeUnit getTimeUnit() {
        return myUnit;
    }

    public void setLength(TimeUnit unit, long length) {
        if (!unit.equals(myUnit)) {
            throw new IllegalArgumentException("Can't convert unit=" + unit
                    + " to my unit=" + myUnit);
        }
        myCount = length;
    }

    public float getLength(TimeUnit unit) {
        if (myUnit.isConstructedFrom(unit)) {
            return (float) myCount * myUnit.getAtomCount(unit);
        } else if (unit.isConstructedFrom(myUnit)) {
            return (float) myCount / unit.getAtomCount(myUnit);
        } else if (!unit.equals(myUnit)) {
            throw new IllegalArgumentException("Can't convert unit=" + unit
                    + " to my unit=" + myUnit);
        }
        return myCount;
    }

    public TaskLength reverse() {
    	return new TaskLengthImpl(getTimeUnit(), -getLength());
    }
    
    public TaskLength translate(TimeUnit toUnit) {
    	float translatedLength = getLength(toUnit);
    	return new TaskLengthImpl(toUnit, translatedLength);
    }
    
    public String toString() {
        return "" + myCount + " " + myUnit.getName();
    }
}
