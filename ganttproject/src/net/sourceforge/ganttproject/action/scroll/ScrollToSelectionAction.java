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
package net.sourceforge.ganttproject.action.scroll;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

public class ScrollToSelectionAction extends GPAction implements TaskSelectionManager.Listener {
  private final TimelineChart myChart;
  private List<Task> mySelectedTasks;

  public ScrollToSelectionAction(UIFacade uiFacade, TimelineChart chart) {
    super("scroll.selection");
    myChart = chart;
    mySelectedTasks = Collections.emptyList();
    uiFacade.getTaskSelectionManager().addSelectionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (mySelectedTasks.isEmpty()) {
      return;
    }
    Date earliestStartDate = null;
    for (Task selectedTask : mySelectedTasks) {
      if (earliestStartDate == null || earliestStartDate.after(selectedTask.getStart().getTime())) {
        earliestStartDate = selectedTask.getStart().getTime();
      }
    }
    myChart.setStartDate(earliestStartDate);
  }

  @Override
  public void selectionChanged(List<Task> currentSelection) {
    mySelectedTasks = currentSelection;
    setEnabled(!currentSelection.isEmpty());
  }

  @Override
  public void userInputConsumerChanged(Object newConsumer) {
  }
}