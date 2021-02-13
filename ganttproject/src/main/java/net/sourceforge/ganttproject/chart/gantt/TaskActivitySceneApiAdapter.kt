package net.sourceforge.ganttproject.chart.gantt

import biz.ganttproject.core.chart.grid.OffsetList
import biz.ganttproject.core.chart.render.AlphaRenderingOption
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.core.time.TimeUnit
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
    get() = model.chartUIConfiguration.isCriticalPathOn && task.isProjectTask
  override val hasNestedTasks
    get() = model.taskManager.taskHierarchy.hasNestedTasks(task)
  override val color: Color
    get() = task.color
  override val shape: ShapePaint?
    get() = task.shape
  override val notes: String?
    get() = task.notes
  override val isMilestone
    get() = (task as TaskImpl).isLegacyMilestone
  override val end: GanttCalendar
    get() = task.end
  override val activities: List<TaskSceneTaskActivity>
    get() = task.activities.map {
      TaskActivityDataImpl(it.isFirst, it.isLast, it.intensity, ITaskSceneTaskImpl(it.owner, model), it.start, it.end, it.duration)
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

  abstract fun getFontBasedRowHeight(): Int

  override fun getRowHeight(): Int {
    var rowHeight = getFontBasedRowHeight()
    if (model.baseline != null) {
      rowHeight += 8
    }
    val appFontSize = model.projectConfig.appFontSize.get()
    return max(appFontSize, rowHeight)
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
