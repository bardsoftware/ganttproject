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
import java.util.ArrayList;
import java.util.List;

import biz.ganttproject.core.time.TimeDuration;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class MoveTaskInteractions extends MouseInteractionBase implements MouseInteraction {
  private final List<Task> myTasks;

  private final List<TaskMutator> myMutators;

  private final UIFacade myUiFacade;

  private final RecalculateTaskScheduleAlgorithm myTaskScheduleAlgorithm;

  public MoveTaskInteractions(MouseEvent e, List<Task> tasks, TimelineFacade chartDateGrid, UIFacade uiFacade,
      RecalculateTaskScheduleAlgorithm taskScheduleAlgorithm) {
    super(chartDateGrid.getDateAt(e.getX()), chartDateGrid);
    myUiFacade = uiFacade;
    myTasks = tasks;
    myTaskScheduleAlgorithm = taskScheduleAlgorithm;
    myMutators = new ArrayList<TaskMutator>(tasks.size());
    for (Task t : tasks) {
      myMutators.add(t.createMutator());
    }
  }

  @Override
  public void apply(MouseEvent event) {
    TimeDuration currentInterval = getLengthDiff(event);
    if (currentInterval.getLength() != 0) {
      for (TaskMutator mutator : myMutators) {
        mutator.shift(currentInterval);
      }
      setStartDate(getChartDateGrid().getDateAt(event.getX()));
    }
  }

  @Override
  public void finish() {
    for (TaskMutator mutator : myMutators) {
      mutator.setIsolationLevel(TaskMutator.READ_COMMITED);
    }
    myUiFacade.getUndoManager().undoableEdit("Task moved", new Runnable() {
      @Override
      public void run() {
        doFinish();
      }
    });
  }

  private void doFinish() {
    for (TaskMutator mutator : myMutators) {
      mutator.commit();
    }
    try {
      myTaskScheduleAlgorithm.run();
    } catch (TaskDependencyException e) {
      myUiFacade.showErrorDialog(e);
    }
    for (Task t : myTasks) {
      t.applyThirdDateConstraint();
    }
    myUiFacade.getActiveChart().reset();
  }
}