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
import biz.ganttproject.app.InternationalizationCoreKt;
import biz.ganttproject.core.calendar.CalendarEvent;
import net.fortuna.ical4j.data.ParserException;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.CalendarEditorPanel;
import net.sourceforge.ganttproject.gui.UIFacade;
import biz.ganttproject.app.WizardPage;
import net.sourceforge.ganttproject.importer.ImporterBase;
import net.sourceforge.ganttproject.importer.ImporterWizardModel;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.prefs.Preferences;

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
  private static final DefaultLocalizer ourLocalizer = InternationalizationCoreKt.getRootLocalizer();
  private CalendarEditorPage myEditorPage;

  public IcsFileImporter() {
    super("impex.ics");
  }

  @Override
  public String getFileNamePattern() {
    return "ics";
  }

  @Override
  public void setContext(IGanttProject project, UIFacade uiFacade, Preferences preferences) {
    super.setContext(project, uiFacade, preferences);
    myEditorPage = new CalendarEditorPage(uiFacade);
  }

  @Override
  public void setModel(@NotNull ImporterWizardModel wizardModel) {
    super.setModel(wizardModel);
    if (myEditorPage != null) {
      myEditorPage.setModel(wizardModel);
    }
  }

  @Override
  public void run() {
//    getUiFacade().getUndoManager().undoableEdit(ourLocalizer.formatText("importCalendar"), new Runnable() {
//      @Override
//      public void run() {
        List<CalendarEvent> events = myEditorPage.getEvents();
        if (events != null) {
          getProject().getActiveCalendar().setPublicHolidays(events);
        }
//      }
//    });
  }


  @Override
  public WizardPage getCustomPage() {
    if (myEditorPage == null) {
      myEditorPage = new CalendarEditorPage(getUiFacade());
    }
    return myEditorPage;
  }

  @Override
  public boolean isReady() {
    return super.isReady() && myEditorPage.getEvents() != null;
  }

  /**
   * Calendar editor page which wraps a {@link CalendarEditorPanel} instance
   */
  static class CalendarEditorPage implements WizardPage {
    private final UIFacade myUiFacade;
    private final JPanel myPanel = new JPanel();
    private List<CalendarEvent> myEvents;
    private ImporterWizardModel myModel;

    public CalendarEditorPage(UIFacade uiFacade) {
      myUiFacade = uiFacade;
    }

    public String getTitle() {
      return ourLocalizer.formatText("impex.ics.previewPage.title");
    }
    public JComponent getComponent() {
      return myPanel;
    }
    void setModel(ImporterWizardModel model) {
      myModel = model;
    }
    List<CalendarEvent> getEvents() {
      return myEvents;
    }

    private File getFile() {
      return myModel.getFile();
    }

    @Override
    public void setActive(boolean b) {
      if (b) {
        myPanel.removeAll();
        var file = getFile();
        if (file != null && file.exists() && file.canRead()) {
          myEvents = readEvents(file);
          if (myEvents != null) {
            myPanel.add(new CalendarEditorPanel(myUiFacade, myEvents, null).createComponent());
            return;
          } else {
            LOGGER.error("No events found in file {}", new Object[]{file}, Collections.emptyMap(), null);
          }
        } else {
          LOGGER.error("File {} is NOT readable", new Object[]{file}, Collections.emptyMap(), null);
        }
        myPanel.add(new JLabel(ourLocalizer.formatText("impex.ics.filePage.error.noEvents", file.getAbsolutePath())));
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
