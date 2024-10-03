/*
Copyright 2013-2020 Dmitry Barashev, BarD Software s.r.o

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

import biz.ganttproject.LoggerApi;
import biz.ganttproject.app.DefaultLocalizer;
import biz.ganttproject.app.InternationalizationKt;
import biz.ganttproject.core.calendar.CalendarEvent;
import net.fortuna.ical4j.data.ParserException;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.calendar.CalendarEditorPanel;
import net.sourceforge.ganttproject.importer.ImporterBase;
import net.sourceforge.ganttproject.wizard.AbstractWizard;
import net.sourceforge.ganttproject.wizard.WizardPage;

import javax.swing.*;
import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * Implements an import wizard plugin responsible for importing ICS files.
 * This plugin adds file chooser page (2nd in the wizard) and calendar editor page (3rd in the wizard)
 *
 * @author dbarashev
 */
public class IcsFileImporter extends ImporterBase {
  private static final LoggerApi LOGGER = GPLogger.create("Import.Ics");
  private static final DefaultLocalizer ourLocalizer = InternationalizationKt.getRootLocalizer();
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
    getUiFacade().getUndoManager().undoableEdit(ourLocalizer.formatText("importCalendar"), new Runnable() {
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
    private final JPanel myPanel = new JPanel();
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
      return ourLocalizer.formatText("impex.ics.previewPage.title");
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
          } else {
            LOGGER.error("No events found in file {}", new Object[]{myFile}, Collections.emptyMap(), null);
          }
        } else {
          LOGGER.error("File {} is NOT readable", new Object[]{myFile}, Collections.emptyMap(), null);
        }
        myPanel.add(new JLabel(ourLocalizer.formatText("impex.ics.filePage.error.noEvents", myFile.getAbsolutePath())));
      }
    }
  }

  /**
   * Reads calendar events from file
   * @return a list of events if file was parsed successfully or null otherwise
   */
  static List<CalendarEvent> readEvents(File f) {
    try {
      return IcsImport.readEvents(new FileInputStream(f));
    } catch (IOException | ParserException e) {
      GPLogger.log(e);
      return null;
    }
  }


}
