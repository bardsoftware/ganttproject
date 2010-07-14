package net.sourceforge.ganttproject.calendar;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.sourceforge.ganttproject.language.GanttLanguage;

public abstract class CalendarFactory {
    public static Calendar newCalendar() {
        return GanttLanguage.getInstance().newCalendar();
    }
}
