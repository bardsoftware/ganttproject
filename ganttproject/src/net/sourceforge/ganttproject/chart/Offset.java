package net.sourceforge.ganttproject.chart;

import java.util.Date;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.time.TimeUnit;

public class Offset {
    private Date myOffsetAnchor;
    private Date myOffsetEnd;
    private int myOffsetPixels;
    private TimeUnit myOffsetUnit;
    private GPCalendar.DayType myDayType;
    private Date myOffsetStart;

    Offset(TimeUnit offsetUnit, Date offsetAnchor, Date offsetStart, Date offsetEnd, int offsetPixels, GPCalendar.DayType dayType) {
        myOffsetAnchor = offsetAnchor;
        myOffsetStart = offsetStart;
        myOffsetEnd = offsetEnd;
        myOffsetPixels = offsetPixels;
        myOffsetUnit = offsetUnit;
        myDayType = dayType;
    }
    Date getOffsetAnchor() {
        return myOffsetAnchor;
    }
    public Date getOffsetStart() {
        return myOffsetStart;
    }
    public Date getOffsetEnd() {
        return myOffsetEnd;
    }
    public int getOffsetPixels() {
        return myOffsetPixels;
    }
    void shift(int pixels) {
        myOffsetPixels += pixels;
    }
    TimeUnit getOffsetUnit() {
        return myOffsetUnit;
    }
    public DayType getDayType() {
        return myDayType;
    }
    public String toString() {
        return "end date: " + myOffsetEnd + " end pixel: " + myOffsetPixels+" time unit: "+myOffsetUnit.getName();
    }
    @Override
    public boolean equals(Object that) {
        if (false==that instanceof Offset) {
            return false;
        }
        Offset thatOffset = (Offset) that;
        return myOffsetPixels==thatOffset.myOffsetPixels &&
               myOffsetEnd.equals(thatOffset.myOffsetEnd) &&
               myOffsetAnchor.equals(thatOffset.myOffsetAnchor);
    }
    @Override
    public int hashCode() {
        return myOffsetEnd.hashCode();
    }
}