package net.sourceforge.ganttproject.time.gregorian;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.time.TextFormatter;
import net.sourceforge.ganttproject.time.TimeUnitText;

public class WeekTextFormatter extends CachingTextFormatter implements
        TextFormatter {
    private Calendar myCalendar;

    WeekTextFormatter(String formatString) {
        myCalendar = CalendarFactory.newCalendar();
    }

    @Override
    protected TimeUnitText createTimeUnitText(Date startDate) {
        myCalendar.setTime(startDate);
        myCalendar.setMinimalDaysInFirstWeek(4);
        Integer weekNo = new Integer(myCalendar.get(Calendar.WEEK_OF_YEAR));
        String shortText = MessageFormat.format("{0}", new Object[] { weekNo });
        String middleText = MessageFormat.format(GanttLanguage.getInstance()
                .getText("week")
                + " {0}", new Object[] { weekNo });
        return new TimeUnitText(middleText, middleText, shortText);
    }

    @Override
    public void languageChanged(Event event) {
        super.languageChanged(event);
        myCalendar = CalendarFactory.newCalendar();
    }



}
