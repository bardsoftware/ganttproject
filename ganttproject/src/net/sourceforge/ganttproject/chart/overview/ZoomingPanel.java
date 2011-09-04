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

import net.sourceforge.ganttproject.action.ZoomInAction;
import net.sourceforge.ganttproject.action.ZoomOutAction;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.UIFacade;

public class ZoomingPanel {
    private final ZoomInAction myZoomInAction;
    private final ZoomOutAction myZoomOutAction;
    private final TimelineChart myChart;

    public ZoomingPanel(UIFacade uiFacade, TimelineChart chart) {
        myZoomInAction = new ZoomInAction(uiFacade.getZoomManager(), "16");
        myZoomOutAction = new ZoomOutAction(uiFacade.getZoomManager(), "16");
        myChart = chart;
    }

    public Component getComponent() {
        return new ToolbarBuilder()
            .withBackground(myChart.getStyle().getSpanningHeaderBackgroundColor())
            .addButton(myZoomInAction).addButton(myZoomOutAction).build();
    }
}