/**
 * 
 */
package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DateInterval;

/**
 * @author nbohn
 */
public class GanttDialogPublicHoliday {

    private DateIntervalListEditor publicHolidayBean;

    private DateIntervalListEditor.DateIntervalModel publicHolidays;

    public GanttDialogPublicHoliday(IGanttProject project) {
        publicHolidays = new DateIntervalListEditor.DefaultDateIntervalModel();
        for (Iterator<Date> iter = project.getActiveCalendar().getPublicHolidays(); iter.hasNext();) {
            Date d = iter.next();
            publicHolidays.add(new DateIntervalListEditor.DateInterval(d,d));
        }

        publicHolidayBean = new DateIntervalListEditor(publicHolidays);
    }

    public Component getContentPane() {
        return publicHolidayBean;
    }

    public List<GanttCalendar> getHolidays() {
    	List<GanttCalendar> result = new ArrayList<GanttCalendar>();
    	DateInterval[] intervals = publicHolidays.getIntervals();
    	for (int i=0; i<intervals.length; i++) {
    		result.add(new GanttCalendar(intervals[i].start));
    	}
    	return result;
    }
}
