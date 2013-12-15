package biz.ganttproject.impex.ical;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.plaf.basic.CalendarState;
import org.osgi.service.prefs.Preferences;

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
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.CalendarEditorPanel;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage;
import net.sourceforge.ganttproject.importer.ImporterBase;

public class IcsFileImporter extends ImporterBase {
  private final CalendarEditorPage myEditorPage;


  public IcsFileImporter() {
    super("impex.ics");
    myEditorPage = new CalendarEditorPage();
  }
 
  @Override
  public String getFileNamePattern() {
    return "ics";
  }

  @Override
  public void run() {
    final File selectedFile = getFile();
    getUiFacade().getUndoManager().undoableEdit("Import", new Runnable() {
      @Override
      public void run() {
        List<CalendarEvent> events = readEvents(selectedFile);
        if (events != null) {
          getProject().getActiveCalendar().setPublicHolidays(events);
        }
      }
    });
  }

  @Override
  public WizardPage[] getMorePages() {
    return new WizardPage[] {myEditorPage};
  }
  
  @Override
  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences preferences) {
    super.setContext(project, uiFacade, preferences);
    myEditorPage.setProjectCalendar(project.getActiveCalendar());
  }

  @Override
  public void setFile(File file) {
    super.setFile(file);
    myEditorPage.setFile(file);
  }



  static class CalendarEditorPage implements WizardPage {
    private File myFile;
    private GPCalendarCalc myCalendar;
    private JPanel myPanel = new JPanel();
    private void setFile(File f) {
      myFile = f;
    }
    public void setProjectCalendar(GPCalendarCalc calendar) {
      myCalendar = calendar;
    }
    public String getTitle() {
      return "Edit calendar";
    }
    public java.awt.Component getComponent() {
      return myPanel;
    }
    
    public void setActive(boolean b) {
      myPanel.removeAll();
      if (myFile != null && myFile.exists() && myFile.canRead()) {
        List<CalendarEvent> events = readEvents(myFile);
        GPCalendarCalc copyCalendar = myCalendar.copy();
        copyCalendar.setPublicHolidays(events);
        myPanel.add(new CalendarEditorPanel(copyCalendar).createComponent());        
      } else {
        myPanel.add(new JLabel(String.format("File %s is not readable", myFile.getAbsolutePath())));
      }
    }    
  }
  
  private static List<CalendarEvent> readEvents(File f) {
    try {
      CalendarBuilder builder = new CalendarBuilder();
      List<CalendarEvent> gpEvents = Lists.newArrayList();
      Calendar c = builder.build(new UnfoldingReader(new FileReader(f)));
      for (Component comp : (List<Component>)c.getComponents()) {
        if (comp instanceof VEvent) {
          VEvent event = (VEvent) comp;
          Date eventStartDate = event.getStartDate().getDate();
          Date eventEndDate = event.getEndDate().getDate();
          TimeDuration oneDay = GPTimeUnitStack.createLength(GPTimeUnitStack.DAY, 1);
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
      return gpEvents;
    } catch (IOException | ParserException e) {
      GPLogger.log(e);
      return null;
    }        
  }
}
