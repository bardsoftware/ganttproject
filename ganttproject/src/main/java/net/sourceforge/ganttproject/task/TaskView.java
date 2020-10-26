/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.task;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * This class keeps task properties which may differ in different views, such as
 * tasks shown in the timeline or tasks expanded in the tree view.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskView {
  private final Set<Task> myTimelineTasks = Sets.newHashSet();

  /**
   * @return a set of tasks which are shown as labels in the timeline
   */
  public Set<Task> getTimelineTasks() {
    return myTimelineTasks;
  }
}
