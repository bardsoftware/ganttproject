package biz.ganttproject.impex.ical;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.jdesktop.swingx.plaf.basic.CalendarState;

import com.google.common.collect.Lists;

import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RRule;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.importer.ImporterBase;

public class IcsFileImporter extends ImporterBase {
  public IcsFileImporter() {
    super("impex.ics");
  }
 
  @Override
  public String getFileNamePattern() {
    return "ics";
  }


  @Override
  public void run(final File selectedFile) {
    getUiFacade().getUndoManager().undoableEdit("Import", new Runnable() {
      @Override
      public void run() {
        CalendarBuilder builder = new CalendarBuilder();
        List<CalendarEvent> gpEvents = Lists.newArrayList();
        try {
          Calendar c = builder.build(new UnfoldingReader(new FileReader(selectedFile)));
          for (Component comp : (List<Component>)c.getComponents()) {
            if (comp instanceof VEvent) {
              VEvent event = (VEvent) comp;
              Date eventStartDate = event.getStartDate().getDate();
              Date eventEndDate = event.getEndDate().getDate();
              TimeDuration oneDay = getProject().getTimeUnitStack().createDuration(GPTimeUnitStack.DAY, 1);
              if (eventEndDate != null) {
                java.util.Date startDate = GPTimeUnitStack.DAY.adjustLeft(eventStartDate);
                java.util.Date endDate = GPTimeUnitStack.DAY.adjustLeft(eventEndDate);
                RRule recurrenceRule = (RRule) event.getProperty(Property.RRULE);
                boolean recursYearly = false;
                if (recurrenceRule != null) {
                  recursYearly = Recur.YEARLY.equals(recurrenceRule.getRecur().getFrequency()) && 1 == recurrenceRule.getRecur().getInterval();
                }
                while (startDate.before(endDate)) {
                  gpEvents.add(CalendarEvent.newEvent(startDate, recursYearly, CalendarEvent.Type.HOLIDAY, event.getSummary().getValue()));
                  startDate = GPCalendarCalc.PLAIN.shiftDate(startDate, oneDay);
                }
              }
            }
          }
          getProject().getActiveCalendar().setPublicHolidays(gpEvents);
        } catch (IOException | ParserException e) {
          GPLogger.log(e);
        }    
      }
    });
  }
}
