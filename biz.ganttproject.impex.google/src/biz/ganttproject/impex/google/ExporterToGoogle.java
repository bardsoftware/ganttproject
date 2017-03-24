/*
Copyright (C) 2017 BarD Software s.r.o., Leonid Shatunov

This file is part of GanttProject, an open-source project management tool.

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
package biz.ganttproject.impex.google;

import biz.ganttproject.core.option.GPOptionGroup;
import com.google.api.services.calendar.model.CalendarListEntry;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.export.ExporterBase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * Exporter to google calendar
 *
 * @author Leonid Shatunov (shatuln@gmail.com)
 */
public class ExporterToGoogle extends ExporterBase {

  private GoogleAuth myAuth = new GoogleAuth();
  private List<CalendarListEntry> myCalendars;

  @Override
  public String getFileTypeDescription() {
    return language.getText("impex.google.description");
  }

  @Override
  public GPOptionGroup getOptions() {
    return null;
  }

  @Override
  public List<GPOptionGroup> getSecondaryOptions() {
    return null;
  }

  @Override
  public String getFileNamePattern() {
    return null;
  }

  @Override
  public String proposeFileExtension() {
    return null;
  }

  @Override
  public String[] getFileExtensions() {
    return new String[0];
  }

  @Override
  public Component getCustomOptionsUI() {
    JPanel result = new JPanel(new BorderLayout());
    result.setBorder(new EmptyBorder(5, 5, 5, 5));
    JButton testConnectionButton = new JButton(new GPAction("googleConnect") {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          myCalendars = myAuth.getAvaliableCalendarList(myAuth.getCalendarService(myAuth.authorize()));
          for (CalendarListEntry i : myCalendars) {
            System.out.println(i.getSummary());
          }
        } catch (Exception e1) {
          GPLogger.getLogger(ExporterToGoogle.class).log(Level.WARNING, "Something went wrong", e1);
        }
      }
    });
    result.add(testConnectionButton, BorderLayout.SOUTH);
    return result;
  }


  @Override
  protected ExporterJob[] createJobs(File outputFile, List<File> resultFiles) {
    return new ExporterJob[0];
  }
}