/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.algorithm.DependencyGraph;

/**
 * Loop detector answers whether a dependency will create a loop in the
 * dependency graph
 *
 * @author dbarashev
 */
public class LoopDetector {
  private final TaskManager myTaskManager;

  public LoopDetector(TaskManager taskManager) {
    myTaskManager = taskManager;
  }

  public boolean isLooping(TaskDependency dep) {
    DependencyGraph graph = myTaskManager.getDependencyGraph();
    DependencyGraph.Logger oldLogger = graph.getLogger();
    graph.startTransaction();
    try {
      graph.setLogger(DependencyGraph.THROWING_LOGGER);
      graph.addDependency(dep);
      return false;
    } catch (TaskDependencyException e) {
      return true;
    } finally {
      graph.rollbackTransaction();
      graph.setLogger(oldLogger);
    }
  }
}
