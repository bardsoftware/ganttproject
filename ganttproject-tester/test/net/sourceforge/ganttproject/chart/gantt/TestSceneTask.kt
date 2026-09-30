/*
Copyright 2026 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.task.Task
import java.awt.Color
import java.util.Date

/**
 * The [ITaskSceneTask] members which a chart test does not normally care about, in one place.
 *
 * The members which a test *does* care about are left unimplemented and throw. A chart test which
 * starts to depend on one of them then fails with the name of the member instead of reading a null
 * out of a property which is declared non-null, which is what the hand-written implementations in
 * the chart tests used to do.
 *
 * The production implementation is `ITaskSceneTaskImpl`; it cannot be used here because it needs a
 * `ChartModel` for the three members which are model-dependent (`isCritical`, `hasNestedTasks` and
 * `notes`), and a chart model in turn needs a UI configuration with fonts and a DPI setting.
 */
abstract class TestSceneTask : ITaskSceneTask {
  override val isCritical: Boolean get() = false
  override val isProjectTask: Boolean get() = false
  override val hasNestedTasks: Boolean get() = false
  override val shape: ShapePaint? get() = null
  override val notes: String? get() = null
  override val completionPercentage: Int get() = 0
  override fun isMilestone(): Boolean = false
  override fun getProperty(propertyID: String?): String? = null

  override val color: Color get() = notImplemented("color")
  override val end: GanttCalendar get() = notImplemented("end")
  override val activities: List<TaskSceneTaskActivity> get() = notImplemented("activities")
  override val duration: TimeDuration get() = notImplemented("duration")
  override val expand: Boolean get() = notImplemented("expand")

  private fun notImplemented(member: String): Nothing =
    throw UnsupportedOperationException("${javaClass.simpleName} does not implement $member")
}

/**
 * A scene task which reads through to a real [Task], for tests which have a task manager. The
 * read-through has to stay live: a test may well change the task after it has wrapped it.
 */
class RealTaskSceneTask(private val task: Task) : TestSceneTask() {
  override fun getRowId(): Int = task.rowId
  override val expand: Boolean get() = task.expand
}

/**
 * A scene task with a single hand-made activity, for tests which drive the scene builder without a
 * task manager and without a chart model.
 */
class SingleActivitySceneTask(
  private val rowId: Int, startDate: Date, endDate: Date, private val length: TimeDuration
) : TestSceneTask() {
  private val ownActivities: List<TaskSceneTaskActivity> =
    listOf(TaskActivityDataImpl(true, true, 1f, this, startDate, endDate, length))

  override fun getRowId(): Int = rowId
  override val color: Color get() = Color.BLUE
  override val end: GanttCalendar = CalendarFactory.createGanttCalendar(endDate)
  override val activities: List<TaskSceneTaskActivity> get() = ownActivities
  override val expand: Boolean get() = true
  override val duration: TimeDuration get() = length
}
