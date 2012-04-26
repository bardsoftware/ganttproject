/*
 * Created on 06.03.2005
 */
package net.sourceforge.ganttproject.chart.timeline;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.time.TimeUnitText;

/**
 * @author bard
 */
public class YearTextFormatter extends CachingTextFormatter implements TimeFormatter {

  private Calendar myCalendar;

  YearTextFormatter() {
    myCalendar = CalendarFactory.newCalendar();
  }

  @Override
  protected TimeUnitText[] createTimeUnitText(Date startDate) {
    myCalendar.setTime(startDate);
    // Integer yearNo = new Integer(myCalendar.get(Calendar.YEAR));
    // String shortText = MessageFormat.format("{0}", new Object[]
    // {yearNo});
    String shortText = MessageFormat.format("{0,date,yyyy}", new Object[] { myCalendar.getTime() });
    return new TimeUnitText[] { new TimeUnitText(shortText) };
  }

}
