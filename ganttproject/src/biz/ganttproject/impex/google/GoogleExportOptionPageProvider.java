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
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.logging.Level;

/**
 * Builds option page for google authorization
 *
 * @author Leonid Shatunov (shatuln@gmail.com)
 */
public class GoogleExportOptionPageProvider extends OptionPageProviderBase {

  private GoogleAuth myAuth = new GoogleAuth();

  public GoogleExportOptionPageProvider() {
    super("impex.google");
  }

  @Override
  public GPOptionGroup[] getOptionGroups() {
    return new GPOptionGroup[0];
  }

  @Override
  public boolean hasCustomComponent() {
    return true;
  }

  public Component buildPageComponent() {
    JPanel result = new JPanel(new BorderLayout());
    result.setBorder(new EmptyBorder(5, 5, 5, 5));
    JButton testConnectionButton = new JButton(new GPAction("googleConnect") {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          myAuth.someSampleWork(myAuth.getCalendarService(myAuth.authorize()));
        } catch (Exception e1) {
          GPLogger.getLogger(GoogleExportOptionPageProvider.class).log(Level.WARNING, "Something went wrong", e1);
        }
      }
    });
    result.add(testConnectionButton, BorderLayout.SOUTH);
    return result;
  }
}