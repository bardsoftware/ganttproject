/*
GanttProject is an opensource project management tool. License: GPL3
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
package net.sourceforge.ganttproject;

import net.sourceforge.ganttproject.plugins.PluginManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

/**
 * This class allow the developer to get some useful references. - GanttProject
 * reference; - CustomColumnManager reference; - CustomColumnStorage reference.
 *
 * @author bbaranne Mar 2, 2005
 */
public class Mediator {
  private static TaskSelectionManager taskSelectionManager = null;

  private static final PluginManager pluginManager = new PluginManager();

  public static void registerTaskSelectionManager(TaskSelectionManager taskSelection) {
    taskSelectionManager = taskSelection;
  }

  @Deprecated
  public static TaskSelectionManager getTaskSelectionManager() {
    return taskSelectionManager;
  }

  public static PluginManager getPluginManager() {
    return pluginManager;
  }
}
