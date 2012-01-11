/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010-2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.chart.overview;

import java.awt.Component;

import javax.swing.AbstractAction;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.scroll.ScrollTimeIntervalAction;
import net.sourceforge.ganttproject.action.scroll.ScrollToEndAction;
import net.sourceforge.ganttproject.action.scroll.ScrollToSelectionAction;
import net.sourceforge.ganttproject.action.scroll.ScrollToStartAction;
import net.sourceforge.ganttproject.action.scroll.ScrollToTodayAction;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.UIFacade;

public class NavigationPanel {
    private final TimelineChart myChart;
    private final IGanttProject myProject;

    private final AbstractAction[] myScrollActions;
    private final AbstractAction myScrollBackAction;
    private final AbstractAction myScrollForwardAction;

    public NavigationPanel(IGanttProject project, TimelineChart chart, UIFacade uiFacade) {
        myProject = project;
        myChart = chart;
        myScrollActions = new AbstractAction[] { new ScrollToStartAction(myProject, myChart),
                new ScrollToTodayAction(myChart), new ScrollToEndAction(myProject, myChart),
                new ScrollToSelectionAction(uiFacade, myChart) };
        myScrollBackAction = new ScrollTimeIntervalAction("scroll.back", -1,
                myProject.getTaskManager(), chart.getModel(), uiFacade.getScrollingManager());
        myScrollForwardAction = new ScrollTimeIntervalAction("scroll.forward", 1,
                myProject.getTaskManager(), chart.getModel(), uiFacade.getScrollingManager());
    }

    public Component getComponent() {
        return new ToolbarBuilder()
            .withBackground(myChart.getStyle().getSpanningHeaderBackgroundColor())
            .addComboBox(myScrollActions, myScrollActions[1])
            .addButton(myScrollBackAction)
            .addButton(myScrollForwardAction)
            .build();
    }

}
