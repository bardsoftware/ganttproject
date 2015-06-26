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
package net.sourceforge.ganttproject.action.view;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;

/**
 * Action to show the options dialog for {@code myChart}.
 */
public class ViewChartOptionsDialogAction extends GPAction {
  private final UIFacade myUIFacade;
  private final ChartComponentBase myChart;

  public ViewChartOptionsDialogAction(ChartComponentBase chart, UIFacade uifacade) {
    super("chart.options");
    myUIFacade = uifacade;
    myChart = chart;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    for (GPOptionGroup group : myChart.getOptionGroups()) {
      group.lock();
    }
    final OkAction okAction = new OkAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        commit();
      }
    };
    final CancelAction cancelAction = new CancelAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        rollback();
      }
    };
    myUIFacade.createDialog(createDialogComponent(), new Action[] { okAction, cancelAction }, "").show();
  }

  private void commit() {
    for (GPOptionGroup group : myChart.getOptionGroups()) {
      group.commit();
    }
  }

  private void rollback() {
    for (GPOptionGroup group : myChart.getOptionGroups()) {
      group.rollback();
    }
  }

  private Component createDialogComponent() {
    OptionsPageBuilder builder = new OptionsPageBuilder();
    builder.setUiFacade(myUIFacade);
    JComponent comp = builder.buildPage(myChart.getOptionGroups(), "ganttChart");
    comp.setBorder(new EmptyBorder(5, 5, 5, 5));
    return comp;
  }

  @Override
  protected String getIconFilePrefix() {
    return "chartOptions_";
  }
}
