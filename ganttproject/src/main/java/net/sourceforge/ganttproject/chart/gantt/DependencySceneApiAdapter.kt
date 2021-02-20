/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sourceforge.ganttproject.chart.gantt

import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskActivity

internal class ITaskImpl(
    private val task: Task,
    private val mapping: (Task) -> ITask?) : ITask {
  override val dependencies: List<IDependency>
    get() {
      return task.dependencies.toArray().map { dep ->
        object : IDependency {
          override val start: ITaskActivity<ITask>
            get() {
              // this cast used to be in TaskRendererImpl2::BarChartConnectorImpl::getStart
              val startActivity = dep.start as TaskActivity
              return TaskActivityDataImpl(
                startActivity.isFirst,
                startActivity.isLast,
                startActivity.intensity,
                mapping(startActivity.owner)!!,
                startActivity.start,
                startActivity.end,
                startActivity.duration
              )
            }
          override val end: ITaskActivity<ITask>
            get() {
              // this cast used to be in TaskRendererImpl2::BarChartConnectorImpl::getEnd
              val endActivity = dep.end as TaskActivity
              return TaskActivityDataImpl(
                endActivity.isFirst, endActivity.isLast, endActivity.intensity, mapping(endActivity.owner)!!,
                endActivity.start, endActivity.end, endActivity.duration
              )
            }
          override val constraintType = dep.constraint.type

          override val hardness = dep.hardness
        }
      }
    }

  override fun isMilestone(): Boolean = task.isMilestone
  override fun getRowId() = task.taskID

  override fun hashCode(): Int {
    return task.hashCode()
  }

  override fun equals(other: Any?): Boolean = other?.let { obj ->
    if (obj === this) {
      return true
    }
    return if (obj is Task) {
      task == obj
    } else {
      false
    }
  } ?: false

}

internal fun tasks2itasks(tasks: List<Task>) : Map<Task, ITask> {
  val result = mutableMapOf<Task, ITask>()
  tasks.map { task -> ITaskImpl(task, result::get).also { result[task] = it } }
  return result
}
