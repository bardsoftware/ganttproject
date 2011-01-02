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

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;

class ToolbarBuilder {
    private final JToolBar myToolbar;
    private final TimelineChart myChart;

    ToolbarBuilder(TimelineChart chart) {
        myChart = chart;
        myToolbar = new JToolBar();
        myToolbar.setBackground(myChart.getStyle().getSpanningHeaderBackgroundColor());
        myToolbar.setFloatable(false);
        myToolbar.setBorderPainted(false);
    }
    
    ToolbarBuilder addButton(Action action) {
        if (myToolbar.getComponentCount() != 0) {
            myToolbar.add(new JLabel(" | "));
        }
        final JButton button = new TestGanttRolloverButton(action);
        button.setIcon(new ImageIcon(getClass().getResource("/icons/blank_big.gif")));
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setBackground(myChart.getStyle().getSpanningHeaderBackgroundColor());
        myToolbar.add(button);
        return this;
    }
    
    JToolBar build() {
        return myToolbar;
    }
}
