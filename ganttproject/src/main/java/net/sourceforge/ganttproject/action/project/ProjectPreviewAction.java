/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action.project;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.print.PrintPreview;

import java.awt.event.ActionEvent;
import java.util.Date;

/**
 * @author bard
 */
public class ProjectPreviewAction extends GPAction {

  private final UIFacade myUIFacade;

  private final GanttProject myProject;

  public ProjectPreviewAction(GanttProject project) {
    super("project.preview");
    myUIFacade = project.getUIFacade();
    myProject = project;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }
    Date startDate, endDate;
    Chart chart = myUIFacade.getActiveChart();
    if (chart == null) {
      myUIFacade.showErrorDialog("Failed to find active chart.\nPlease report this problem to GanttProject development team");
      return;
    }

    try {
      startDate = chart.getStartDate();
      endDate = chart.getEndDate();
    } catch (UnsupportedOperationException exception) {
      startDate = null;
      endDate = null;
    }

    try {
      PrintPreview preview = new PrintPreview(myProject, myUIFacade, chart, startDate, endDate);
      preview.setVisible(true);
    } catch (OutOfMemoryError exception) {
      myUIFacade.showErrorDialog(getI18n("printing.out_of_memory"));
      return;
    }
  }

  @Override
  protected String getIconFilePrefix() {
    return "preview_";
  }
}
