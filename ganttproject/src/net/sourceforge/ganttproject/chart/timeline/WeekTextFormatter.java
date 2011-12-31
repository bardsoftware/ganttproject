package net.sourceforge.ganttproject.chart.timeline;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.time.TimeUnitText;

public class WeekTextFormatter extends CachingTextFormatter implements
        TimeFormatter {
    private Calendar myCalendar;

    WeekTextFormatter() {
        myCalendar = CalendarFactory.newCalendar();
    }

    @Override
    protected TimeUnitText[] createTimeUnitText(Date startDate) {
        myCalendar.setTime(startDate);
        myCalendar.setMinimalDaysInFirstWeek(4);
        return new TimeUnitText[] {
                createTopText(), createBottomText()
        };
    }

    private TimeUnitText createTopText() {
        Integer weekNo = new Integer(myCalendar.get(Calendar.WEEK_OF_YEAR));
        String shortText = weekNo.toString();
        String middleText = MessageFormat.format("{0} {1}", GanttLanguage.getInstance().getText("week"), weekNo);
        String longText = middleText;
        return new TimeUnitText(longText, middleText, shortText);
    }

    private TimeUnitText createBottomText() {
        return new TimeUnitText(GanttLanguage.getInstance().getShortDateFormat().format(myCalendar.getTime()));
    }
    @Override
    public void languageChanged(Event event) {
        super.languageChanged(event);
        myCalendar = CalendarFactory.newCalendar();
    }



}
