/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.view;

import java.awt.Component;
import java.awt.Container;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.ViewToggleAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.view.GPView;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.plugins.PluginManager;

/**
 * Collection of actions present in the view menu
 */
public class ViewMenu extends JMenu {
    public ViewMenu(final IGanttProject project, GPViewManager viewManager, String key) {
        super(GPAction.createVoidAction(key));

        List<Chart> charts = PluginManager.getCharts();
        if (charts.isEmpty()) {
            setEnabled(false);
        }
        for (Chart chart : charts) {
            chart.init(project);
            GPView view = new GPViewImpl(chart);
            viewManager.createView(view, null);
            add(new JCheckBoxMenuItem(new ViewToggleAction(chart, viewManager, view)));
        }
    }

    private static class GPViewImpl implements GPView {
        private final Chart myChart;
        private Component myComponent;

        GPViewImpl(Chart chart) {
            myChart = chart;
            myComponent = (Component) chart.getAdapter(Container.class);
        }
        @Override
        public void setActive(boolean active) {
        }

        @Override
        public Chart getChart() {
            return myChart;
        }
        @Override
        public Component getViewComponent() {
            return myComponent;
        }

    }
}
