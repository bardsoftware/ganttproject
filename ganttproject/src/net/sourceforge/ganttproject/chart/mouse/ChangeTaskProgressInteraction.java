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

import biz.ganttproject.core.calendar.walker.WorkingUnitCounter;
import net.sourceforge.ganttproject.chart.TaskChartModelFacade;
import net.sourceforge.ganttproject.chart.TaskInteractionHintRenderer;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskMutator;

import java.awt.*;
import java.awt.event.MouseEvent;

public class ChangeTaskProgressInteraction extends MouseInteractionBase implements MouseInteraction {
  private final TaskProgressChartItem myTaskProgrssItem;

  private final TaskMutator myMutator;

  private TaskInteractionHintRenderer myLastNotes;

  private final WorkingUnitCounter myCounter;

  private UIFacade myUiFacade;

  private ChangeTaskProgressRuler myChangeScale;

  private int myStartPixel;

  public ChangeTaskProgressInteraction(MouseEvent e, TaskProgressChartItem taskProgress, TimelineFacade chartDateGrid,
      TaskChartModelFacade taskFacade, UIFacade uiFacade) {
    super(taskProgress.getTask().getStart().getTime(), chartDateGrid);
    myStartPixel = e.getX();
    myUiFacade = uiFacade;
    myTaskProgrssItem = taskProgress;
    myMutator = myTaskProgrssItem.getTask().createMutator();
    myChangeScale = new ChangeTaskProgressRuler(taskProgress.getTask(), taskFacade);
    myCounter = new WorkingUnitCounter(getChartDateGrid().getCalendar(), getTask().getDuration().getTimeUnit());
  }

  private Task getTask() {
    return myTaskProgrssItem.getTask();
  }

  @Override
  public void apply(MouseEvent event) {
    int newProgress = myChangeScale.getProgress(event.getX());
    if (newProgress > 100) {
      newProgress = 100;
    }
    if (newProgress < 0) {
      newProgress = 0;
    }
    myMutator.setCompletionPercentage(newProgress);
    myLastNotes = new TaskInteractionHintRenderer(newProgress + "%", event.getX(), event.getY() - 30);
  }

  @Override
  public void finish() {
    myMutator.setIsolationLevel(TaskMutator.READ_COMMITED);
    myUiFacade.getUndoManager().undoableEdit("Task progress changed", new Runnable() {
      @Override
      public void run() {
        doFinish(myMutator);
      }
    });
    myUiFacade.getActiveChart().reset();
  }

  private void doFinish(TaskMutator mutator) {
    mutator.commit();
    myLastNotes = null;
  }

  @Override
  public void paint(Graphics g) {
    if (myLastNotes != null) {
      myLastNotes.paint((Graphics2D)g);
    }
  }
}
