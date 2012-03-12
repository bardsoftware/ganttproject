/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.item.TaskRegularAreaChartItem;
import net.sourceforge.ganttproject.chart.mouse.MouseInteraction;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.task.Task;

public interface ChartImplementation extends ZoomListener {
  void paintChart(Graphics g);

  // void paintComponent(Graphics g, List<Task> visibleTasks);

  MouseListener getMouseListener();

  MouseMotionListener getMouseMotionListener();

  void beginChangeTaskEndInteraction(MouseEvent initiatingEvent, TaskBoundaryChartItem taskBoundary);

  MouseInteraction getActiveInteraction();

  void beginChangeTaskStartInteraction(MouseEvent e, TaskBoundaryChartItem taskBoundary);

  MouseInteraction finishInteraction();

  void beginChangeTaskProgressInteraction(MouseEvent e, TaskProgressChartItem item);

  void beginDrawDependencyInteraction(MouseEvent initiatingEvent, TaskRegularAreaChartItem taskArea);

  // void beginMoveTaskInteraction(MouseEvent e, Task task);

  void beginMoveTaskInteractions(MouseEvent e, List<Task> tasks);

  void beginScrollViewInteraction(MouseEvent e);

}