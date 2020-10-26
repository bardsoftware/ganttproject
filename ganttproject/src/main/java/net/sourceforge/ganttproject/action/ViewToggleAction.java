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
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.view.GPView;
import net.sourceforge.ganttproject.gui.view.GPViewManager;

/**
 * @author bard
 */
public class ViewToggleAction extends GPAction {
  private final Chart myChart;

  private final GPView myView;

  private final GPViewManager myViewManager;

  public ViewToggleAction(Chart chart, GPViewManager viewManager, GPView view) {
    myChart = chart;
    myView = view;
    myViewManager = viewManager;
    updateAction();
  }

  @Override
  protected String getLocalizedDescription() {
    return MessageFormat.format(getI18n("view.toggle.description"), getLocalizedName());
  }

  @Override
  protected String getLocalizedName() {
    return myChart == null ? null : myChart.getName();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    myViewManager.toggleVisible(myView);
  }
}
