/**
 *
 */
package net.sourceforge.ganttproject.calendar;

import java.util.Date;

import net.sourceforge.ganttproject.GanttCalendar;

/**
 * @author nbohn
 */
public class GanttDaysOff {
    private final GanttCalendar myStart, myFinish;

    public GanttDaysOff(Date start, Date finish) {
        myStart = new GanttCalendar(start);
        myFinish = new GanttCalendar(finish);
    }
    public GanttDaysOff(GanttCalendar start, GanttCalendar finish) {
        myStart = new GanttCalendar(start.getYear(), start.getMonth(), start
                .getDate());
        myFinish = finish;
    }

    public String toString() {
        return (myStart + " -> " + myFinish);
    }

    public boolean equals(GanttDaysOff dayOffs) {
        return ((dayOffs.getStart().equals(myStart)) && (dayOffs.getFinish()
                .equals(myFinish)));
    }

    public GanttCalendar getStart() {
        return myStart;
    }

    public GanttCalendar getFinish() {
        return myFinish;
    }

    public boolean isADayOff(GanttCalendar date) {
        return (date.equals(myStart) || date.equals(myFinish) || (date
                .before(myFinish) && date.after(myStart)));
    }

    public boolean isADayOff(Date date) {
        return (date.equals(myStart.getTime())
                || date.equals(myFinish.getTime()) || (date.before(myFinish
                .getTime()) && date.after(myStart.getTime())));
    }

    public int isADayOffInWeek(Date date) {
        GanttCalendar start = myStart.Clone();
        GanttCalendar finish = myFinish.Clone();
        for (int i = 0; i < 7; i++) {
            start.add(-1);
            finish.add(-1);
            if (date.equals(start.getTime())
                    || date.equals(finish.getTime())
                    || (date.before(finish.getTime()) && date.after(start
                            .getTime())))
                return i + 1;
        }
        return -1;
    }

    public int getDuration() {
        return (myStart.diff(myFinish)) + 1;
    }

    public static GanttDaysOff create(GanttDaysOff from) {
        return new GanttDaysOff(from.myStart.Clone(), from.myFinish.Clone());
    }

}
