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

import biz.ganttproject.core.chart.render.ShapeConstants
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.chart.scene.IdentifiableRow
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder
import biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder
import biz.ganttproject.core.time.*
import com.google.common.base.Strings
import java.awt.Color
import java.util.*

typealias TaskSceneTaskActivity = ITaskActivity<ITaskSceneTask>

interface ITaskSceneTask : IdentifiableRow {
  val isCritical: Boolean
  val isProjectTask: Boolean
  val hasNestedTasks: Boolean
  val color: Color
  val shape: ShapePaint?
  val notes: String?
  val end: GanttCalendar
  val activities: List<TaskSceneTaskActivity>
  val expand: Boolean
  val duration: TimeDuration
  val completionPercentage: Int

  fun isMilestone(): Boolean
  fun getProperty(propertyID: String?): Any?
}

class TaskActivityDataImpl<T : IdentifiableRow>(
  override val isFirst: Boolean, override val isLast: Boolean,
  override val intensity: Float, private val _owner: T,
  private val _start: Date, private val _end: Date,
  private val _duration: TimeDuration
) : ITaskActivity<T>() {
  override fun getStart() = _start
  override fun getEnd() = _end
  override fun getDuration() = _duration
  override fun getOwner() = _owner
}

internal class TaskSceneMilestoneActivity(
  val task: ITaskSceneTask,
  private val _start: Date, private val _end: Date,
  private val _duration: TimeDuration
) : TaskSceneTaskActivity() {
  override val isFirst = true
  override val isLast = true
  override val intensity = 1f

  override fun getStart() = _start
  override fun getEnd() = _end
  override fun getDuration() = _duration
  override fun getOwner() = task
}

internal class TaskSceneTaskActivityImpl(
  private val task: ITaskSceneTask,
  private val _start: Date, private val _end: Date,
  private val _duration: TimeDuration,
  override val intensity: Float
) : TaskSceneTaskActivity() {
  constructor(task: ITaskSceneTask, _start: Date, _end: Date, _duration: TimeDuration) : this(task, _start, _end, _duration, 1f)

  override val isFirst = this === owner.activities.first()
  override val isLast = this === owner.activities.last()

  override fun getStart() = _start
  override fun getEnd() = _end
  override fun getDuration() = _duration
  override fun getOwner() = task
}

internal class TaskActivitySceneTaskApi : TaskActivitySceneBuilder.TaskApi<ITaskSceneTask, TaskSceneTaskActivity> {
  override fun isFirst(activity: TaskSceneTaskActivity): Boolean {
    return activity.isFirst
  }

  override fun isLast(activity: TaskSceneTaskActivity): Boolean {
    return activity.isLast
  }

  override fun isVoid(activity: TaskSceneTaskActivity): Boolean {
    return activity.intensity == 0f
  }

  override fun isCriticalTask(task: ITaskSceneTask): Boolean {
    return task.isCritical
  }

  override fun isProjectTask(task: ITaskSceneTask): Boolean {
    return task.isProjectTask
  }

  override fun isMilestone(task: ITaskSceneTask): Boolean {
    return task.isMilestone()
  }

  override fun hasNestedTasks(task: ITaskSceneTask): Boolean {
    return task.hasNestedTasks
  }

  override fun getColor(task: ITaskSceneTask): Color {
    return task.color
  }

  override fun getShapePaint(task: ITaskSceneTask): ShapePaint {
    return task.shape ?: ShapeConstants.TRANSPARENT
  }

  override fun hasNotes(task: ITaskSceneTask): Boolean {
    return !Strings.isNullOrEmpty(task.notes)
  }
}

internal class TaskLabelSceneTaskApi : TaskLabelSceneBuilder.TaskApi<ITaskSceneTask> {
  override fun getProperty(task: ITaskSceneTask, propertyID: String?): Any? {
    return task.getProperty(propertyID)
  }
}
