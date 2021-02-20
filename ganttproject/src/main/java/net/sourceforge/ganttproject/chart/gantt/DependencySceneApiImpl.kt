package net.sourceforge.ganttproject.chart.gantt

import biz.ganttproject.core.chart.scene.BarChartActivity
import biz.ganttproject.core.chart.scene.BarChartConnector
import biz.ganttproject.core.chart.scene.IdentifiableRow
import biz.ganttproject.core.chart.scene.gantt.Connector
import biz.ganttproject.core.chart.scene.gantt.DependencySceneBuilder
import com.google.common.collect.Lists
import biz.ganttproject.core.model.task.ConstraintType
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import java.awt.Dimension
import java.util.*

abstract class ITaskActivity<T : IdentifiableRow> : BarChartActivity<T> {
  abstract val isFirst: Boolean
  abstract val isLast: Boolean
  abstract val intensity: Float

  override fun equals(obj: Any?): Boolean {
    if (obj == null) {
      return false;
    }
    if (obj === this) {
      return true
    }
    return if (obj is BarChartActivity<*>) {
      obj.owner.rowId == owner.rowId && obj.start == this.start
    } else false
  }

  override fun hashCode(): Int {
    return start.hashCode()
  }
}

internal class TaskActivityPart<T : IdentifiableRow>(
    val original: ITaskActivity<T>,
    val _start: Date,
    val _end: Date,
    val _duration: TimeDuration) : ITaskActivity<T>(){
  override fun getStart() = _start
  override fun getEnd() = _end
  override fun getDuration() = _duration
  override fun getOwner() = original.owner

  override val isFirst: Boolean
    get() = original.isFirst && _start == original.start
  override val isLast: Boolean
    get() = original.isLast && _end == original.end
  override val intensity: Float
    get() = original.intensity
}

interface IDependency {
  val start: ITaskActivity<ITask>
  val end: ITaskActivity<ITask>
  val constraintType: ConstraintType
  val hardness: TaskDependency.Hardness
}

interface ITask : IdentifiableRow {
  val dependencies: List<IDependency>
  fun isMilestone(): Boolean
}

internal class BarChartConnectorImpl(
    internal val dependency: IDependency,
    private val splitter: ITaskActivitySplitter<ITask>) : BarChartConnector<ITask, BarChartConnectorImpl> {

  override fun getStart(): BarChartActivity<ITask> {
    val startActivity = dependency.start
    val splitActivities = splitter.split(listOf(startActivity))
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
    val endActivity = dependency.end
    val splitActivities = splitter.split(listOf(endActivity))
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

internal interface ITaskActivitySplitter<T : IdentifiableRow> {
  fun split(activities: List<ITaskActivity<T>>): List<ITaskActivity<T>>
}

internal class DependencySceneTaskApi(
  private val taskList: List<ITask>,
  private val splitter: ITaskActivitySplitter<ITask>) : DependencySceneBuilder.TaskApi<ITask, BarChartConnectorImpl> {
  override fun isMilestone(task: ITask): Boolean {
    return task.isMilestone()
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
      result.add(BarChartConnectorImpl(d, splitter))
    }
    return result
  }

  override fun getTasks(): List<ITask> = taskList
}


