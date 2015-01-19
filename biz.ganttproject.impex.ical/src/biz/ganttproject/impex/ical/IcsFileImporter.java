/*
Copyright 2013 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.impex.ical;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
import net.fortuna.ical4j.util.CompatibilityHints;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.calendar.CalendarEditorPanel;
import net.sourceforge.ganttproject.importer.ImporterBase;
import net.sourceforge.ganttproject.wizard.AbstractWizard;
import net.sourceforge.ganttproject.wizard.WizardPage;
import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

import com.google.common.collect.Lists;

/**
 * Implements an import wizard plugin responsible for importing ICS files. 
 * This plugin adds file chooser page (2nd in the wizard) and calendar editor page (3rd in the wizard)
 *   
 * @author dbarashev
 */
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
    getUiFacade().getUndoManager().undoableEdit("Import", new Runnable() {
      @Override
      public void run() {
        List<CalendarEvent> events = myEditorPage.getEvents();
        if (events != null) {
          getProject().getActiveCalendar().setPublicHolidays(events);
        }
      }
    });
  }

  
  @Override
  public WizardPage getCustomPage() {
    return myEditorPage;
  }

  @Override
  public boolean isReady() {
    return super.isReady() && myEditorPage.getEvents() != null;
  }

  @Override
  public void setFile(File file) {
    super.setFile(file);
    myEditorPage.setFile(file);    
    if (file != null && file.exists() && file.canRead()) {
      myEditorPage.setEvents(readEvents(file));
    }
  }

  /**
   * Calendar editor page which wraps a {@link CalendarEditorPanel} instance
   */
  static class CalendarEditorPage implements WizardPage {
    private File myFile;
    private JPanel myPanel = new JPanel();
    private List<CalendarEvent> myEvents;
    private void setFile(File f) {
      myFile = f;
    }
    void setEvents(List<CalendarEvent> events) {
      myEvents = events;
    }
    List<CalendarEvent> getEvents() {
      return myEvents;
    }
    
    public String getTitle() {
      return "Edit calendar";
    }
    public JComponent getComponent() {
      return myPanel;
    }
    
    public void setActive(AbstractWizard wizard) {
      if (wizard != null) {
        myPanel.removeAll();
        if (myFile != null && myFile.exists() && myFile.canRead()) {
          if (myEvents != null) {
            myPanel.add(new CalendarEditorPanel(wizard.getUIFacade(), myEvents, null).createComponent());
            return;
          }
        }
        myPanel.add(new JLabel(String.format("File %s is not readable", myFile.getAbsolutePath())));
      }
    }    
  }
  
  /**
   * Reads calendar events from file
   * @return a list of events if file was parsed successfully or null otherwise
   */
  private static List<CalendarEvent> readEvents(File f) {
    try {
      CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
      CalendarBuilder builder = new CalendarBuilder();
      List<CalendarEvent> gpEvents = Lists.newArrayList();
      Calendar c = builder.build(new UnfoldingReader(new FileReader(f)));
      for (Component comp : (List<Component>)c.getComponents()) {
        if (comp instanceof VEvent) {
          VEvent event = (VEvent) comp;
          if (event.getStartDate() == null) {
            GPLogger.log("No start date found, ignoring. Event="+event);
            continue;            
          }
          Date eventStartDate = event.getStartDate().getDate();
          if (event.getEndDate() == null) {
            GPLogger.log("No end date found, using start date instead. Event="+event);
          }
          Date eventEndDate = event.getEndDate() == null ? eventStartDate : event.getEndDate().getDate();
          TimeDuration oneDay = GPTimeUnitStack.createLength(GPTimeUnitStack.DAY, 1);
          if (eventEndDate != null) {
            java.util.Date startDate = GPTimeUnitStack.DAY.adjustLeft(eventStartDate);
            java.util.Date endDate = GPTimeUnitStack.DAY.adjustLeft(eventEndDate);
            RRule recurrenceRule = (RRule) event.getProperty(Property.RRULE);
            boolean recursYearly = false;
            if (recurrenceRule != null) {
              recursYearly = Recur.YEARLY.equals(recurrenceRule.getRecur().getFrequency()) && 1 == recurrenceRule.getRecur().getInterval();
            }
            while (startDate.compareTo(endDate) <= 0) {
              gpEvents.add(CalendarEvent.newEvent(startDate, recursYearly, CalendarEvent.Type.HOLIDAY, event.getSummary().getValue(), null));
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
