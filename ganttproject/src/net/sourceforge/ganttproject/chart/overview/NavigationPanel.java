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

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.TaskLength;

public class NavigationPanel {
    private final TimelineChart myChart;
    private final IGanttProject myProject;

    public NavigationPanel(IGanttProject project, TimelineChart chart, UIFacade workbenchFacade) {
        myProject = project;
        myChart = chart;
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
        return new ToolbarBuilder(myChart).addButton(new ScrollToProjectStart()).addButton(new ScrollToToday())
            .addButton(new ScrollToProjectEnd()).build();
    }

	protected TaskLength createTimeInterval(int i) {
		return myProject.getTaskManager().createLength(i);
	}
}
