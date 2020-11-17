/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart.mouse;

import java.awt.event.MouseEvent;
import java.util.Date;

import biz.ganttproject.core.time.CalendarFactory;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;

public class ChangeTaskStartInteraction extends ChangeTaskBoundaryInteraction implements MouseInteraction {
  private TaskMutator myMutator;

  public ChangeTaskStartInteraction(MouseEvent e, TaskBoundaryChartItem taskBoundary, TimelineFacade chartDateGrid,
      UIFacade uiFacade, RecalculateTaskScheduleAlgorithm taskScheduleAlgorithm) {

    super(taskBoundary.getTask().getEnd().getTime(), taskBoundary.getTask(), chartDateGrid, uiFacade,
        taskScheduleAlgorithm);
    myMutator = getTask().createMutator();
  }

  @Override
  public void apply(MouseEvent e) {
    Date dateUnderX = getChartDateGrid().getDateAt(e.getX());
    if (!dateUnderX.equals(getStartDate()) && dateUnderX.before(getTask().getEnd().getTime())) {
      myMutator.setStart(CalendarFactory.createGanttCalendar(dateUnderX));
      getTask().applyThirdDateConstraint();
    }
    updateTooltip(e);
  }

  @Override
  public void finish() {
    super.finish(myMutator);
    getTask().applyThirdDateConstraint();
  }

  @Override
  protected String getNotesText() {
    return getTask().getStart().toString();
  }
}