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

import biz.ganttproject.core.chart.grid.OffsetList
import biz.ganttproject.core.chart.render.AlphaRenderingOption
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.chart.ChartModel
import net.sourceforge.ganttproject.chart.ChartModelImpl
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskImpl
import net.sourceforge.ganttproject.task.TaskProperties
import java.awt.Color
import java.util.*
import kotlin.math.max

internal class ITaskSceneTaskImpl(private val task: Task, private val model: ChartModel) : ITaskSceneTask {
  private val props = TaskProperties(model.timeUnitStack)

  override fun getRowId() = task.taskID
  override val isCritical
    get() = model.chartUIConfiguration.isCriticalPathOn && task.isCritical
  override val isProjectTask
    get() = task.isProjectTask
  override val hasNestedTasks
    get() = model.taskManager.taskHierarchy.hasNestedTasks(task)
  override val color: Color
    get() = task.color
  override val shape: ShapePaint?
    get() = task.shape
  override val notes: String?
    get() = task.notes

  override fun isMilestone() = (task as TaskImpl).isLegacyMilestone
  override val end: GanttCalendar
    get() = task.end
  override val activities: List<TaskSceneTaskActivity>
    get() = task.activities.map {
      TaskActivityDataImpl(it.isFirst, it.isLast, it.intensity, this, it.start, it.end, it.duration)
    }
  override val expand
    get() = task.expand
  override val duration: TimeDuration
    get() = task.duration
  override val completionPercentage: Int
    get() = task.completionPercentage

  override fun getProperty(propertyID: String?): Any? {
    return props.getProperty(task, propertyID)
  }

  override fun hashCode() = task.hashCode()

  override fun equals(other: Any?): Boolean = other?.let { obj ->
    if (obj === this) {
      return true
    }
    if (obj is ITaskSceneTaskImpl) {
      return this.task.taskID == obj.task.taskID
    }
    return if (obj is Task) {
      task == obj
    } else {
      false
    }
  } ?: false
}

internal abstract class TaskActivitySceneChartApi(
  private val model: ChartModelImpl
) : TaskActivitySceneBuilder.ChartApi {
  override fun getChartStartDate(): Date {
    return model.offsetAnchorDate
  }

  override fun getEndDate(): Date {
    return model.endDate
  }

  override fun getBottomUnitOffsets(): OffsetList {
    return model.bottomUnitOffsets
  }

  override fun getViewportWidth(): Int {
    return model.bounds.width
  }

  override fun getWeekendOpacityOption(): AlphaRenderingOption {
    return model.chartUIConfiguration.weekendAlphaValue
  }
}

internal fun mapTaskSceneTask2Task(tasks: List<Task>, model: ChartModel) : Map<ITaskSceneTask, Task> {
  val result = mutableMapOf<ITaskSceneTask, Task>()
  tasks.map { task -> ITaskSceneTaskImpl(task, model).also { result[it] = task } }
  return result
}
