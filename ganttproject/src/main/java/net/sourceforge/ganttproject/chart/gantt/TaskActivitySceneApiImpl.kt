package net.sourceforge.ganttproject.chart.gantt

import biz.ganttproject.core.chart.render.ShapeConstants
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.chart.scene.IdentifiableRow
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder
import biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder
import biz.ganttproject.core.time.*
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import com.google.common.base.Strings
import java.awt.Color
import java.util.*

internal typealias TaskSceneTaskActivity = ITaskActivity<ITaskSceneTask>

internal interface ITaskSceneTask : IdentifiableRow {
  val isCritical: Boolean
  val isProjectTask: Boolean
  val hasNestedTasks: Boolean
  val color: Color
  val shape: ShapePaint?
  val notes: String?
  val isMilestone: Boolean
  val end: GanttCalendar
  val activities: List<TaskSceneTaskActivity>
  val expand: Boolean
  val duration: TimeDuration
  val completionPercentage: Int

  fun getProperty(propertyID: String?): Any?
}

internal class TaskActivityDataImpl<T : IdentifiableRow>(
  override val isFirst: Boolean, override val isLast: Boolean, override val intensity: Float,
  val _owner: T, val _start: Date, val _end: Date, val _duration: TimeDuration
) : ITaskActivity<T>() {
  override fun getStart() = _start
  override fun getEnd() = _end
  override fun getDuration() = _duration
  override fun getOwner() = _owner
}

internal class TaskSceneMilestoneActivity(
  val task: ITaskSceneTask, val _start: Date, val _end: Date, val _duration: TimeDuration
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
  val task: ITaskSceneTask, val _start: Date, val _end: Date, val _duration: TimeDuration, override val intensity: Float
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
    return task.isMilestone
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
