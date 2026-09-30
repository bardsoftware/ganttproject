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

import biz.ganttproject.core.calendar.AlwaysWorkingTimeCalendarImpl
import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.GPCalendarCalc
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.Painter
import biz.ganttproject.core.chart.grid.Offset
import biz.ganttproject.core.chart.grid.OffsetList
import biz.ganttproject.core.chart.render.AlphaRenderingOption
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.chart.scene.gantt.TaskActivitySceneBuilder
import biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.core.time.TimeUnit
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.customproperty.CustomColumnsManager
import biz.ganttproject.customproperty.CustomPropertyManager
import net.sourceforge.ganttproject.GanttPreviousStateTask

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Counts the baseline bars which the gantt chart scene actually contains. The scene is built by the
 * production [GanttChartSceneBuilder] over a hand-made input; the bars are counted by walking the
 * finished canvas with a [Painter], which is the same public entry point the real painter uses.
 *
 * The point of interest is a task which kept its end date but changed its duration.
 */
class BaselineBarCountTest {
  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
  }

  /**
   * The positive control. The task ends two days later than in the baseline, which is the deviation
   * the chart has always shown. If this one does not count a bar then the test rig is broken and
   * says nothing about the case below.
   */
  @Test
  fun `a task whose end date moved gets a baseline bar`() {
    val scene = scene(baselineStart = AUG_10, baselineDuration = 5, taskStart = AUG_12, taskDuration = 5)
    assertEquals(1, scene.baselineBarCount, "the end date moved, so a baseline bar is expected")
    assertTrue("later" in scene.baselineBarStyles, "the task now ends two days later than planned")
  }

  /**
   * The case under measurement: the baseline lasted five days and ended on 15 August; the task now
   * starts five days earlier and lasts ten days, so it ends on the very same day. Something did
   * change and the chart is the only place where the user could see it.
   */
  @Test
  fun `a task which kept its end date but doubled its duration gets a baseline bar`() {
    val scene = scene(baselineStart = AUG_10, baselineDuration = 5, taskStart = AUG_5, taskDuration = 10)
    // Guards the scenario itself: if these two dates were not equal we would be measuring
    // something else entirely.
    assertEquals(
      scene.baselineEnd, scene.taskEnd,
      "the scenario requires equal end dates; both are computed by the same calendar"
    )
    assertTrue(scene.baselineDurationDays != scene.taskDurationDays, "the scenario requires unequal durations")
    assertEquals(1, scene.baselineBarCount, "the duration changed, so a baseline bar is expected")
    // The deadline is met, so the bar carries neither of the deviation colours.
    assertFalse("later" in scene.baselineBarStyles, "the end date did not move, so nothing is late")
    assertFalse("earlier" in scene.baselineBarStyles, "the end date did not move, so nothing is early")
  }
}

private val AUG_1 = day(2026, Calendar.AUGUST, 1)
private val AUG_5 = day(2026, Calendar.AUGUST, 5)
private val AUG_10 = day(2026, Calendar.AUGUST, 10)
private val AUG_12 = day(2026, Calendar.AUGUST, 12)

private fun day(year: Int, month: Int, dayOfMonth: Int): Date = GregorianCalendar(year, month, dayOfMonth).time

private const val ROW_ID = 1
private const val DAY_WIDTH = 20
private const val ROW_HEIGHT = 20

/** The result of one scene build: the numbers the assertions are made on. */
private class SceneUnderTest(
  val baselineBarCount: Int,
  val baselineBarStyles: Set<String>,
  val baselineEnd: Date,
  val taskEnd: Date,
  val baselineDurationDays: Int,
  val taskDurationDays: Int
)

private fun days(count: Int): TimeDuration = GPTimeUnitStack.createLength(GPTimeUnitStack.DAY, count.toFloat())

private fun scene(baselineStart: Date, baselineDuration: Int, taskStart: Date, taskDuration: Int): SceneUnderTest {
  val calendar: GPCalendarCalc = AlwaysWorkingTimeCalendarImpl()
  val taskEnd = calendar.shiftDate(taskStart, days(taskDuration))
  val baselineEnd = calendar.shiftDate(baselineStart, days(baselineDuration))

  val task = SceneTaskStub(ROW_ID, taskStart, taskEnd, days(taskDuration))
  val baseline = listOf(
    GanttPreviousStateTask(ROW_ID, CalendarFactory.createGanttCalendar(baselineStart), baselineDuration, false, false)
  )
  val canvas = Canvas()
  GanttChartSceneBuilder(InputApiStub(calendar, task, baseline), canvas).render()

  val bars = collectBaselineBars(canvas)
  return SceneUnderTest(
    baselineBarCount = bars.size,
    baselineBarStyles = bars.flatMap { bar -> DEVIATION_STYLES.filter { bar.hasStyle(it) } }.toSet(),
    baselineEnd = baselineEnd,
    taskEnd = taskEnd,
    baselineDurationDays = baselineDuration,
    taskDurationDays = task.duration.length
  )
}

/**
 * Walks the canvas and every one of its layers -- the baseline bars live on a layer of their own --
 * and counts the shapes which the renderer marked as a baseline bar. The style is the *primary*
 * style set with `setStyle`, which `hasStyle` does not see.
 */
private fun collectBaselineBars(canvas: Canvas): List<Canvas.Shape> {
  val bars = mutableListOf<Canvas.Shape>()
  val collector = object : Painter {
    override fun prePaint() = Unit
    override fun paint(rectangle: Canvas.Rectangle) = tally(rectangle)
    override fun paint(line: Canvas.Line) = Unit
    override fun paint(next: Canvas.Text) = Unit
    override fun paint(textGroup: Canvas.TextGroup) = Unit
    override fun paint(rhombus: Canvas.Rhombus) = tally(rhombus)
    fun tally(shape: Canvas.Shape) {
      if (shape.style == "previousStateTask") {
        bars.add(shape)
      }
    }
  }
  canvas.paint(collector)
  canvas.layers.forEach { it.paint(collector) }
  return bars
}

private val DEVIATION_STYLES = listOf("later", "earlier", "milestone")

private class SceneTaskStub(
  private val id: Int, startDate: Date, endDate: Date, private val length: TimeDuration
) : ITaskSceneTask {
  private val ownActivities: List<TaskSceneTaskActivity> =
    listOf(TaskActivityDataImpl(true, true, 1f, this, startDate, endDate, length))

  override fun getRowId(): Int = id
  override val isCritical: Boolean get() = false
  override val isProjectTask: Boolean get() = false
  override val hasNestedTasks: Boolean get() = false
  override val color: Color get() = Color.BLUE
  override val shape: ShapePaint? get() = null
  override val notes: String? get() = null
  override val end: GanttCalendar = CalendarFactory.createGanttCalendar(endDate)
  override val activities: List<TaskSceneTaskActivity> get() = ownActivities
  override val expand: Boolean get() = true
  override val duration: TimeDuration get() = length
  override val completionPercentage: Int get() = 0
  override fun isMilestone(): Boolean = false
  override fun getProperty(propertyID: String?): String? = null
}

private class InputApiStub(
  private val calendar: GPCalendarCalc,
  private val task: ITaskSceneTask,
  private val baseline: List<GanttPreviousStateTask>
) : GanttChartSceneBuilder.InputApi {
  private val timeUnitStack = GPTimeUnitStack()
  private val customPropertyManager = CustomColumnsManager()
  private val offsets = dailyOffsets(AUG_1, 40)
  private val chartEnd = GregorianCalendar(2026, Calendar.SEPTEMBER, 20).time

  override fun getHeaderHeight(): Int = 0
  override fun getWidth(): Int = 1000
  override fun getLabelsFontSize(): Int = 10
  override fun getVerticalOffset(): Int = 0
  override fun getTasksUnitOffsets(): OffsetList = offsets
  override fun getProgressBarTimeUnit(): TimeUnit = GPTimeUnitStack.DAY
  override fun getVerticalPartitioning(): VerticalPartitioning =
    VerticalPartitioning(listOf(task)) { _, _ -> true }

  override fun getVisibleTasks(): List<ITask> = listOf()
  override fun getVisibleTaskSceneTasks(): List<ITaskSceneTask> = listOf(task)
  override fun getTasksInDocumentOrder(): List<ITaskSceneTask> = listOf(task)
  override fun getBaseline(): List<GanttPreviousStateTask> = baseline

  override fun getChartApi(labelsRenderer: TaskLabelSceneBuilder<ITaskSceneTask>): TaskActivitySceneBuilder.ChartApi =
    object : TaskActivitySceneBuilder.ChartApi {
      override fun getChartStartDate(): Date = AUG_1
      override fun getEndDate(): Date = chartEnd
      override fun getBottomUnitOffsets(): OffsetList = offsets
      override fun getRowHeight(): Int = ROW_HEIGHT
      override fun getBarHeight(): Int = 10
      override fun getViewportWidth(): Int = 1000
      override fun getWeekendOpacityOption(): AlphaRenderingOption = AlphaRenderingOption()
    }

  override fun getCalendar(): GPCalendarCalc = calendar
  override fun getStartDate(): Date = AUG_1
  override fun createLength(timeUnit: TimeUnit, startDate: Date, endDate: Date): TimeDuration =
    timeUnitStack.createDuration(timeUnit, startDate, endDate)

  override fun createLength(duration: Int): TimeDuration = days(duration)
  override fun getCustomPropertyManager(): CustomPropertyManager = customPropertyManager
}

/** One offset per calendar day, [DAY_WIDTH] pixels wide, so that a bar has somewhere to land. */
private fun dailyOffsets(chartStart: Date, dayCount: Int): OffsetList {
  val result = OffsetList()
  val walker = GregorianCalendar()
  walker.time = chartStart
  for (i in 0 until dayCount) {
    val offsetStart = walker.time
    walker.add(Calendar.DAY_OF_YEAR, 1)
    val offsetEnd = walker.time
    result.add(
      Offset(
        GPTimeUnitStack.DAY, chartStart, offsetStart, offsetEnd,
        i * DAY_WIDTH, (i + 1) * DAY_WIDTH, GPCalendar.DayMask.WORKING
      )
    )
  }
  return result
}
