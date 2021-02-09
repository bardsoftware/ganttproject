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
package net.sourceforge.ganttproject.chart

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.render.ShapeConstants
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.chart.scene.BarChartActivity
import biz.ganttproject.core.chart.scene.BarChartConnector
import biz.ganttproject.core.chart.scene.gantt.Connector
import biz.ganttproject.core.chart.scene.gantt.DependencySceneBuilder
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder
import biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder
import biz.ganttproject.core.option.DefaultEnumerationOption
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.core.time.TimeDurationImpl
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness
import java.awt.Color
import java.awt.Dimension
import java.awt.Rectangle
import java.util.*
import java.util.concurrent.TimeUnit

class UnboundedViewportTaskSceneBuilder(
  private val input: TaskActivitySceneBuilder.ChartApi,
  private val logger: ((String) -> Unit)? = null
) {
  fun build(tasks: List<Task>, canvas: Canvas = Canvas()): Canvas {
    val allTasks = tasks.flatMap { it.flatten() }
    paintTasks(allTasks, canvas)
    paintDependencies(allTasks, canvas)
    return canvas
  }

  private fun paintTasks(tasks: List<Task>, canvas: Canvas) {
    val taskBarOffset = (input.rowHeight - input.barHeight) / 2
    val taskActivityScene = TaskActivitySceneBuilder(
      ActivitySceneTaskApi, input, canvas, TaskLabelSceneBuilderStub, TaskActivitySceneBuilder.Style(taskBarOffset)
    )
    tasks.forEachIndexed { row, task ->
      taskActivityScene.renderActivities(row, listOf(task.activity), input.bottomUnitOffsets)
      val nextLineY = row * input.rowHeight
      val nextLine = canvas.createLine(0, nextLineY, input.viewportWidth, nextLineY)
      nextLine.foregroundColor = Color.GRAY
    }
  }

  private fun paintDependencies(tasks: List<Task>, canvas: Canvas) {
    val dependencyRenderer = DependencySceneBuilder(
      canvas, canvas.newLayer(), DependencySceneTaskApi(tasks, logger), { input.barHeight }
    )
    dependencyRenderer.build()
  }

  abstract class Task {
    abstract val start: Date
    abstract val end: Date
    abstract val isMilestone: Boolean
    abstract val subtasks: List<Task>
    abstract val dependants: List<Dependant>

    val activity = Activity()

    fun flatten(result: MutableList<Task> = mutableListOf()): List<Task> {
      result.add(this)
      subtasks.forEach { it.flatten(result) }
      return result
    }

    inner class Activity : BarChartActivity<Task> {
      override fun getStart() = this@Task.start
      override fun getEnd() = this@Task.end
      override fun getOwner() = this@Task

      override fun getDuration(): TimeDuration {
        return TimeDurationImpl(
          GPTimeUnitStack.DAY,
          TimeUnit.DAYS.convert(start.time - end.time, TimeUnit.MILLISECONDS)
        )
      }
    }

    class Dependant(val task: Task, val type: TaskDependencyConstraint.Type, val hardness: Hardness)
  }

  private class DependencySceneTaskApi(
    private val tasks: List<Task>,
    private val logger: ((String) -> Unit)?
  ) : DependencySceneBuilder.TaskApi<Task, TaskDependency> {
    override fun isMilestone(task: Task) = task.isMilestone
    override fun getTasks() = tasks

    override fun getUnitVector(activity: BarChartActivity<Task>, connector: TaskDependency): Dimension? {
      return when (activity) {
        connector.start -> connector.startVector
        connector.end -> connector.endVector
        else -> null.also { logger?.invoke("Connector-activity mismatch") }
      }
    }

    override fun getStyle(dependency: TaskDependency): String {
      return if (dependency.dependant.hardness === Hardness.STRONG) "dependency.line.hard" else "dependency.line.rubber"
    }

    override fun getConnectors(task: Task): Iterable<TaskDependency> {
      return task.dependants.map { TaskDependency(task, it) }
    }
  }

  private class TaskDependency(
    private val dependee: Task, val dependant: Task.Dependant
  ) : BarChartConnector<Task, TaskDependency> {
    override fun getImpl(): TaskDependency = this
    override fun getStart() = dependee.activity
    override fun getEnd() = dependant.task.activity

    override fun getStartVector(): Dimension {
      return if (dependant.type == TaskDependencyConstraint.Type.finishfinish || dependant.type == TaskDependencyConstraint.Type.finishstart)
        Connector.Vector.EAST
      else
        Connector.Vector.WEST
    }

    override fun getEndVector(): Dimension {
      return if (dependant.type == TaskDependencyConstraint.Type.finishfinish || dependant.type == TaskDependencyConstraint.Type.startfinish)
        Connector.Vector.EAST
      else
        Connector.Vector.WEST
    }
  }

  private object ActivitySceneTaskApi : TaskActivitySceneBuilder.TaskApi<Task, Task.Activity> {
    override fun isFirst(activity: Task.Activity) = true
    override fun isLast(activity: Task.Activity) = true
    override fun isVoid(activity: Task.Activity) = false
    override fun isCriticalTask(task: Task) = false
    override fun isProjectTask(task: Task) = false
    override fun isMilestone(task: Task) = task.isMilestone
    override fun hasNestedTasks(task: Task) = task.subtasks.isNotEmpty()
    override fun hasNotes(task: Task) = false
    override fun getShapePaint(task: Task): ShapePaint = ShapeConstants.TRANSPARENT

    override fun getColor(task: Task): Color {
      return if (task.isMilestone || task.subtasks.isNotEmpty()) Color.BLACK else GanttProjectImpl.DEFAULT_TASK_COLOR
    }
  }

  private object TaskLabelSceneBuilderStub : TaskLabelSceneBuilder<Task>(TaskApiStub, InputApiStub, Canvas()) {
    override fun stripVerticalLabelSpace(nextBounds: Rectangle) {}

    private object InputApiStub : InputApi {
      private val option = DefaultEnumerationOption("", emptyArray<Int>())

      override fun getTopLabelOption() = option
      override fun getBottomLabelOption() = option
      override fun getLeftLabelOption() = option
      override fun getRightLabelOption() = option
      override fun getFontSize() = 0
    }

    private object TaskApiStub : TaskApi<Task> {
      override fun getProperty(task: Task, propertyID: String) = task
    }
  }
}
