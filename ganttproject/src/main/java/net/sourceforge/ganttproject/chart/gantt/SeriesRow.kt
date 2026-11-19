/*
Copyright 2020 Dmitry Kazakov, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.chart.gantt

import biz.ganttproject.task.recurrence.isRecurrenceGroup
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade

/**
 * The tasks which a recurrence series draws in the row of its group while the group is collapsed: the source task
 * and its repetitions, earliest first. Empty for any other task, and for a group which is expanded, because then
 * every member has a row of its own.
 *
 * The group counts as collapsed when none of its members has a row in [rows], the list of tasks the task table shows.
 * [biz.ganttproject.core.model.task.Task.expand] is not asked: the task table does not write it back when a group is
 * opened or closed.
 */
fun collapsedSeriesMembers(task: Task, hierarchy: TaskContainmentHierarchyFacade, rows: Collection<Task>): List<Task> {
  if (!task.isRecurrenceGroup()) {
    return emptyList()
  }
  val members = hierarchy.getNestedTasks(task)
  if (members.isEmpty() || members.any { it in rows }) {
    return emptyList()
  }
  return members.sortedBy { it.start.time }
}
