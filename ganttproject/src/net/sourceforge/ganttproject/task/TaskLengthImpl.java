/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
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

    public int getLength() {
        return (int) myCount;
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
