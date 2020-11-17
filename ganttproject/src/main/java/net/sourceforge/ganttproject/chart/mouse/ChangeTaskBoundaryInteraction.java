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

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.chart.TaskInteractionHintRenderer;
import net.sourceforge.ganttproject.chart.mouse.MouseInteraction.TimelineFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Date;

public abstract class ChangeTaskBoundaryInteraction extends MouseInteractionBase {
  private TaskInteractionHintRenderer myLastNotes;

  private final Task myTask;

  private final UIFacade myUiFacade;

  private final RecalculateTaskScheduleAlgorithm myTaskScheduleAlgorithm;

  protected ChangeTaskBoundaryInteraction(Date startDate, Task task, TimelineFacade chartDateGrid, UIFacade uiFacade,
      RecalculateTaskScheduleAlgorithm taskScheduleAlgorithm) {
    super(startDate, chartDateGrid);
    myTask = task;
    myUiFacade = uiFacade;
    myTaskScheduleAlgorithm = taskScheduleAlgorithm;
  }

  protected void updateTooltip(MouseEvent e) {
    if (myLastNotes == null) {
      myLastNotes = new TaskInteractionHintRenderer("", e.getX(), e.getY());
    }
    myLastNotes.setText(getNotesText());
    myLastNotes.setX(e.getX());
  }

  protected Task getTask() {
    return myTask;
  }

  public void finish(final TaskMutator mutator) {
    mutator.setIsolationLevel(TaskMutator.READ_COMMITED);
    myUiFacade.getUndoManager().undoableEdit("Task boundary changed", new Runnable() {
      @Override
      public void run() {
        doFinish(mutator);
      }
    });
  }

  private void doFinish(TaskMutator mutator) {
    mutator.commit();
    myLastNotes = null;
    try {
      myTaskScheduleAlgorithm.run();
    } catch (TaskDependencyException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
      myUiFacade.showErrorDialog(e);
    }
    myUiFacade.getActiveChart().reset();
  }

  @Override
  public void paint(Graphics g) {
    if (myLastNotes != null) {
      myLastNotes.paint((Graphics2D)g);
    }
  }

  protected abstract String getNotesText();
}
