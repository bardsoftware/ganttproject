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
package biz.ganttproject.task.recurrence

import biz.ganttproject.task.ancestors
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade

/**
 * A collapsed recurrence group draws the bars of its members in its own row. The bar under the pointer belongs to a
 * member, so a press selects the member, and the task table opens every ancestor of a selection which did not come
 * from the table: the group would open and the row would turn back into one row per member. Whoever causes a
 * selection can implement this interface to say that its rows must stay as they are.
 */
interface CollapsedSeriesRows {
  /** True if [task] is a group whose row draws the bars of its members at the moment. */
  fun drawsSeriesInOneRow(task: Task): Boolean
}

/**
 * The ancestors of [selection] which the task table may open, outermost first. Only a [source] which implements
 * [CollapsedSeriesRows] keeps groups closed; every other source, the search box among them, gets all ancestors.
 */
fun ancestorsToOpen(selection: List<Task>, hierarchy: TaskContainmentHierarchyFacade, source: Any?): List<Task> {
  val outermostFirst = ancestors(selection, hierarchy).reversed()
  val chart = source as? CollapsedSeriesRows ?: return outermostFirst
  return outermostFirst.filterNot { chart.drawsSeriesInOneRow(it) }
}
