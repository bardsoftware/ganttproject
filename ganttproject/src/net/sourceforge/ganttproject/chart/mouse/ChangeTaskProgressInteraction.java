/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.awt.Graphics;
import java.awt.event.MouseEvent;

import net.sourceforge.ganttproject.calendar.walker.WorkingUnitCounter;
import net.sourceforge.ganttproject.chart.TaskInteractionHintRenderer;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskMutator;

public class ChangeTaskProgressInteraction extends MouseInteractionBase implements
        MouseInteraction {
    private final TaskProgressChartItem myTaskProgrssItem;

    private final TaskMutator myMutator;

    private TaskInteractionHintRenderer myLastNotes;

    private final WorkingUnitCounter myCounter;

    private UIFacade myUiFacade;

    public ChangeTaskProgressInteraction(MouseEvent e,
            TaskProgressChartItem taskProgress, TimelineFacade chartDateGrid, UIFacade uiFacade) {
        super(taskProgress.getTask().getStart().getTime(), chartDateGrid);
        myUiFacade = uiFacade;
        myTaskProgrssItem = taskProgress;
        myMutator = myTaskProgrssItem.getTask().createMutator();
        myCounter = new WorkingUnitCounter(getChartDateGrid().getCalendar(), getTask().getDuration().getTimeUnit());
    }

    private Task getTask() {
        return myTaskProgrssItem.getTask();
    }

    public void apply(MouseEvent event) {
        TaskLength currentInterval = myCounter.run(getStartDate(), getChartDateGrid().getDateAt(event.getX()));
        int newProgress = (int) (100 * currentInterval.getValue() / getTask().getDuration().getValue());
        if (newProgress > 100) {
            newProgress = 100;
        }
        if (newProgress < 0) {
            newProgress = 0;
        }
        myMutator.setCompletionPercentage(newProgress);
        myLastNotes = new TaskInteractionHintRenderer(newProgress + "%", event.getX(), event.getY() - 30);
    }

    public void finish() {
        myMutator.setIsolationLevel(TaskMutator.READ_COMMITED);
        myUiFacade.getUndoManager().undoableEdit("Task progress changed",
                new Runnable() {
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

    public void paint(Graphics g) {
        if (myLastNotes != null) {
            myLastNotes.paint(g);
        }
    }
}