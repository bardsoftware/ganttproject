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
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
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
            ScrollToProjectStart() {
                super("scrollToStart");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                myChart.setStartDate(myProject.getTaskManager().getProjectStart());
                myChart.scrollBy(createTimeInterval(-1));
            }
        }
        class ScrollToProjectEnd extends GPAction {
            ScrollToProjectEnd() {
                super("scrollToEnd");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                Date projectEnd = myProject.getTaskManager().getProjectEnd();
                myChart.setStartDate(projectEnd);
                while(projectEnd.before(myChart.getEndDate())) {
                    myChart.scrollBy(createTimeInterval(-1));
                }
                myChart.scrollBy(createTimeInterval(1));
            }
        }
        class ScrollToToday extends GPAction {
            ScrollToToday() {
                super("scrollToToday");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                myChart.setStartDate(new Date());
            }
        }
        class ScrollToSelection extends GPAction implements TaskSelectionManager.Listener {
            ScrollToSelection() {
                super("scrollToSelection");
                myUiFacade.getTaskSelectionManager().addSelectionListener(this);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                Date earliestStartDate = null;
                List<Task> selectedTasks = myUiFacade.getTaskSelectionManager().getSelectedTasks();
                if (selectedTasks == null || selectedTasks.isEmpty()) {
                    return;
                }
                for (Task selectedTask : selectedTasks) {
                    if (earliestStartDate == null || earliestStartDate.after(selectedTask.getStart().getTime())) {
                        earliestStartDate = selectedTask.getStart().getTime();
                    }
                }
                myChart.setStartDate(earliestStartDate);
            }
            @Override
            public void selectionChanged(List<Task> currentSelection) {
                setEnabled(!currentSelection.isEmpty());
            }
            @Override
            public void userInputConsumerChanged(Object newConsumer) {
            }
        }
        Action[] scrollActions = new Action[] {
            new ScrollToProjectStart(), new ScrollToToday(), new ScrollToProjectEnd(), new ScrollToSelection()};
        class ScrollTimeIntervalAction extends GPAction {
            private final ScrollingManager myScrollingManager;
            private final int myIntervalLength;

            ScrollTimeIntervalAction(String name, int intervalLength, ScrollingManager scrollingManager) {
                super(name);
                myIntervalLength = intervalLength;
                myScrollingManager = scrollingManager;
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                myScrollingManager.scrollBy(myProject.getTaskManager().createLength(
                    myChart.getModel().getBottomUnit(), myIntervalLength));
            }
            @Override
            protected String getLocalizedName() {
                return MessageFormat.format("<html><b>{0}</b></html>", getI18n(getID()));
            }

        }
        return new ToolbarBuilder()
            .withBackground(myChart.getStyle().getSpanningHeaderBackgroundColor())
            .addComboBox(scrollActions, scrollActions[1])
            .addButton(new ScrollTimeIntervalAction("backDate", -1, myUiFacade.getScrollingManager()))
            .addButton(new ScrollTimeIntervalAction("forwardDate", 1, myUiFacade.getScrollingManager()))
            .build();
    }

    protected TaskLength createTimeInterval(int i) {
        return myProject.getTaskManager().createLength(i);
    }
}
