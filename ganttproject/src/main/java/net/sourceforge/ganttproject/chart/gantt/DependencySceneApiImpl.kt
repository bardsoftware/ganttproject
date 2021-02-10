package net.sourceforge.ganttproject.chart.gantt

import biz.ganttproject.core.chart.scene.BarChartActivity
import biz.ganttproject.core.chart.scene.BarChartConnector
import biz.ganttproject.core.chart.scene.gantt.Connector
import biz.ganttproject.core.chart.scene.gantt.DependencySceneBuilder
import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import net.sourceforge.ganttproject.chart.TaskActivityPart
//import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskActivity
import biz.ganttproject.core.model.task.ConstraintType
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import java.awt.Dimension
import java.util.*

internal interface IDependency {
  val start: TaskActivity
  val end: TaskActivity
  val constraintType: ConstraintType
  val hardness: TaskDependency.Hardness
}

internal interface ITask {
  val dependencies: List<IDependency>
  val isMilestone: Boolean
}
internal class BarChartConnectorImpl(
    internal val dependency: IDependency,
    private val chartStartDate: Date,
    private val chartEndDate: Date
    ) : BarChartConnector<ITask, BarChartConnectorImpl> {

  override fun getStart(): BarChartActivity<ITask> {
    val startActivity = dependency.start as TaskActivity
    val splitActivities: List<TaskActivity> = splitOnBounds(listOf(startActivity), chartStartDate, chartEndDate)
    assert(splitActivities.size > 0) {
      String.format(
        "It is expected that split activities length is >= 1 for dep=%s",
        dependency.toString()
      )
    }
    val type = dependency.constraintType
    return if (type == ConstraintType.finishfinish || type == ConstraintType.finishstart) {
      splitActivities[splitActivities.size - 1]
    } else {
      splitActivities[0]
    }
  }

  override fun getEnd(): BarChartActivity<ITask> {
    val endActivity = dependency.end as TaskActivity
    val splitActivities: List<TaskActivity> = splitOnBounds(listOf(endActivity), chartStartDate, chartEndDate)
    assert(splitActivities.size > 0) {
      String.format(
        "It is expected that split activities length is >= 1 for dep=%s",
        dependency.toString()
      )
    }
    val type = dependency.constraintType
    return if (type == ConstraintType.finishfinish || type == ConstraintType.finishstart) {
      splitActivities[0]
    } else {
      splitActivities[splitActivities.size - 1]
    }
  }

  override fun getImpl(): BarChartConnectorImpl {
    return this
  }

  override fun getStartVector(): Dimension {
    val type = dependency.constraintType
    return if (type == ConstraintType.finishfinish || type == ConstraintType.finishstart) {
      Connector.Vector.EAST
    } else Connector.Vector.WEST
  }

  override fun getEndVector(): Dimension {
    val type = dependency.constraintType
    return if (type == ConstraintType.finishfinish || type == ConstraintType.startfinish) {
      Connector.Vector.EAST
    } else Connector.Vector.WEST
  }
}

internal class DependencySceneTaskApi(
  private val taskList: List<ITask>,
  private val chartStartDate: Date,
  private val chartEndDate: Date) : DependencySceneBuilder.TaskApi<ITask, BarChartConnectorImpl> {
  override fun isMilestone(task: ITask): Boolean {
    return task.isMilestone
  }

  override fun getUnitVector(
    activity: BarChartActivity<ITask?>,
    connector: BarChartConnectorImpl
  ): Dimension? {
    return if (activity == connector.getStart()) {
      connector.getStartVector()
    } else if (activity == connector.getEnd()) {
      connector.getEndVector()
    } else {
      assert(false) { String.format("Should not be here. activity=%s, connector=%s", activity, connector) }
      null
    }
  }

  override fun getStyle(dependency: BarChartConnectorImpl): String? {
    return if (dependency.dependency.hardness === TaskDependency.Hardness.STRONG) "dependency.line.hard" else "dependency.line.rubber"
  }

  override fun getConnectors(task: ITask): Iterable<BarChartConnectorImpl>? {
    val deps = task.dependencies
    val result: MutableList<BarChartConnectorImpl> = Lists.newArrayListWithCapacity(deps.size)
    for (d in deps) {
      result.add(BarChartConnectorImpl(d, chartStartDate, chartEndDate))
    }
    return result
  }

  override fun getTasks(): List<ITask> = taskList
}

/**
 * Some parts of the renderer, e.g. progress bar rendering, don't like activities which cross
 * the viewport borders. The reason is that we build shapes (specifically, rectangles) only for
 * visible parts of activities. When activity crosses the viewport border, the invisible parts
 * are no more than ~20px wide. However, progress bar needs to know pixel size of all shapes from
 * the task beginning up to the point where progress bar should be terminated OR needs activities
 * to be split exactly at the viewport border.
 *
 * @param activities
 * @return
 */

/**
 * This method scans the list of activities and splits activities crossing the borders
 * of the given frame into parts "before" and "after" the border date. Activities which
 * do not cross frame borders are left as is, and the relative order of activities is preserved.
 *
 * Normally no more than two activities from the input list are partitioned.
 *
 * @return input activities with those crossing frame borders partitioned into left and right parts
 */
fun splitOnBounds(activities: List<TaskActivity>, frameStartDate: Date, frameEndDate: Date): List<TaskActivity> {
  Preconditions.checkArgument(
    frameEndDate.compareTo(frameStartDate) >= 0,
    String.format("Invalid frame: start=%s end=%s", frameStartDate, frameEndDate)
  )
  val result: MutableList<TaskActivity> = Lists.newArrayList()
  val queue: Deque<TaskActivity> = LinkedList(activities)
  while (!queue.isEmpty()) {
    val head = queue.pollFirst()
    if (head.start.compareTo(frameStartDate) < 0
      && head.end.compareTo(frameStartDate) > 0
    ) {

      // Okay, this activity crosses frame start. Lets add its left part to the result
      // and push back its right part
      val beforeViewport: TaskActivity = TaskActivityPart(head, head.start, frameStartDate)
      val remaining: TaskActivity = TaskActivityPart(head, frameStartDate, head.end)
      result.add(beforeViewport)
      queue.addFirst(remaining)
      continue
    }
    if (head.start.compareTo(frameEndDate) < 0
      && head.end.compareTo(frameEndDate) > 0
    ) {
      // This activity crosses frame end date. Again, lets add its left part to the result
      // and push back the remainder.
      val insideViewport: TaskActivity = TaskActivityPart(head, head.start, frameEndDate)
      val remaining: TaskActivity = TaskActivityPart(head, frameEndDate, head.end)
      result.add(insideViewport)
      queue.addFirst(remaining)
      continue
    }
    result.add(head)
  }
  return result
}

