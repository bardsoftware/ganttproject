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
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author nbohn
 */
public class GanttDialogPublicHoliday {
    private GanttLanguage lang = GanttLanguage.getInstance();

    private DateIntervalListEditor publicHolidayBean;


    private DateIntervalListEditor.DateIntervalModel publicHolidays;

    private boolean isChanged = false;

    private UIFacade myUIFacade;
    
    public GanttDialogPublicHoliday(IGanttProject project, UIFacade uiFacade) {
        publicHolidays = new DateIntervalListEditor.DefaultDateIntervalModel();
        for (Iterator<Date> iter = project.getActiveCalendar().getPublicHolidays().iterator(); iter.hasNext();) {
            Date d = iter.next();
            publicHolidays.add(new DateIntervalListEditor.DateInterval(d,d));
        }

        //publicHolidayBean = new GanttPublicHolidayBean(publicHolidays);
        publicHolidayBean = new DateIntervalListEditor(publicHolidays);
        myUIFacade = uiFacade;

        //publicHolidayBean.addActionListener(this);

    }

    public Component getContentPane() {
        return publicHolidayBean;
    }
    
    public List<GanttCalendar> getHolidays() {
        //return Arrays.asList(publicHolidays.toArray());
    	List<GanttCalendar> result =new ArrayList<GanttCalendar>();
    	DateInterval[] intervals = publicHolidays.getIntervals();
    	for (int i=0; i<intervals.length; i++) {
    		result.add(new GanttCalendar(intervals[i].start));
    	}
    	return result;
    }
}
