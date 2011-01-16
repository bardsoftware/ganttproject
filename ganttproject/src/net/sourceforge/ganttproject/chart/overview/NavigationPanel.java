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
package net.sourceforge.ganttproject.chart.overview;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import javax.swing.Action;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent.SelectionListener;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

public class NavigationPanel {
    private final TimelineChart myChart;
    private final IGanttProject myProject;
    private final UIFacade myUiFacade;

    public NavigationPanel(IGanttProject project, TimelineChart chart, UIFacade uiFacade) {
        myProject = project;
        myChart = chart;
        myUiFacade = uiFacade;
    }

    public Component getComponent() {
        class ScrollToProjectStart extends GPAction {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChart.setStartDate(myProject.getTaskManager().getProjectStart());
                myChart.scrollBy(createTimeInterval(-1));
            }
            @Override
            protected String getLocalizedName() {
                return MessageFormat.format("<html><b>&nbsp;{0}&nbsp;</b></html>", getI18n("start"));
            }
        }
        class ScrollToProjectEnd extends GPAction {
            @Override
            public void actionPerformed(ActionEvent e) {
                Date projectEnd = myProject.getTaskManager().getProjectEnd();
                myChart.setStartDate(projectEnd);
                while(projectEnd.before(myChart.getEndDate())) {
                    myChart.scrollBy(createTimeInterval(-1));
                }
                myChart.scrollBy(createTimeInterval(1));
            }
            @Override
            protected String getLocalizedName() {
                return MessageFormat.format("<html><b>&nbsp;{0}&nbsp;</b></html>", getI18n("end"));
            }
        }
        class ScrollToToday extends GPAction {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChart.setStartDate(new Date());
            }
            @Override
            protected String getLocalizedName() {
                return MessageFormat.format("<html><b>&nbsp;{0}&nbsp;</b></html>", "Today");
            }
        }
        class ScrollToSelection extends GPAction implements TaskSelectionManager.Listener {
            ScrollToSelection() {
                myUiFacade.getTaskSelectionManager().addSelectionListener(this);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                Date earliestStartDate = null;
                for (Task selectedTask : myUiFacade.getTaskSelectionManager().getSelectedTasks()) {
                    if (earliestStartDate == null || earliestStartDate.after(selectedTask.getStart().getTime())) {
                        earliestStartDate = selectedTask.getStart().getTime();
                    }
                }
                myChart.setStartDate(earliestStartDate);
            }
            @Override
            protected String getLocalizedName() {
                return MessageFormat.format("<html><b>&nbsp;{0}&nbsp;</b></html>", "Selection");
            }
            @Override
            public void selectionChanged(List<Task> currentSelection) {
                setEnabled(!currentSelection.isEmpty());
            }
            @Override
            public void userInputConsumerChanged(Object newConsumer) {
            }
        }
        return new ToolbarBuilder(myChart)
            .addComboBox(new Action[] {
                            new ScrollToProjectStart(), new ScrollToToday(), new ScrollToProjectEnd(), new ScrollToSelection()})
            .build();
//        return new ToolbarBuilder(myChart).addButton(new ScrollToProjectStart()).addButton(new ScrollToToday())
//            .addButton(new ScrollToProjectEnd()).addButton(new ScrollToSelection()).build();
    }

	protected TaskLength createTimeInterval(int i) {
		return myProject.getTaskManager().createLength(i);
	}
}
