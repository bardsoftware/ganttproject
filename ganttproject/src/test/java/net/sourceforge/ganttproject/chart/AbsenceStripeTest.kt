/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

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
package net.sourceforge.ganttproject.chart

import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.Painter
import biz.ganttproject.core.chart.grid.Offset
import biz.ganttproject.core.option.DefaultFontOption
import biz.ganttproject.core.option.DefaultIntegerOption
import biz.ganttproject.core.option.FontSpec
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.task.recurrence.toModelDate
import biz.ganttproject.task.recurrence.toModelLocalDate
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.gui.UIConfiguration
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Dimension
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * Absence stripes on a task bar: which days of the bar are striped, and where the stripe is drawn.
 */
class AbsenceStripeTest {
  init {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : CalendarFactory.LocaleApi {
          override fun getLocale(): Locale = Locale.GERMANY
          override fun getShortDateFormat(): DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMANY)
        })
      }
    }
  }

  private val dayWidth = 20
  private val monday = LocalDate.of(2026, 9, 7)
  private val tuesday = monday.plusDays(1)
  private val wednesday = monday.plusDays(2)
  private val thursday = monday.plusDays(3)
  private val friday = monday.plusDays(4)
  private val saturday = monday.plusDays(5)

  private data class Box(val style: String?, val left: Int, val width: Int, val top: Int, val height: Int)

  private class Collector : Painter {
    val boxes = mutableListOf<Box>()
    override fun prePaint() {}
    override fun paint(rectangle: Canvas.Rectangle) {
      if (rectangle.isVisible) {
        boxes.add(Box(rectangle.style, rectangle.leftX, rectangle.width, rectangle.topY, rectangle.height))
      }
    }
    override fun paint(line: Canvas.Line) {}
    override fun paint(next: Canvas.Text) {}
    override fun paint(textGroup: Canvas.TextGroup) {}
    override fun paint(rhombus: Canvas.Rhombus) {}
  }

  private inner class Chart {
    val builder = TestSetupHelper.newTaskManagerBuilder().withCalendar(WeekendCalendarImpl())
    val taskManager = builder.build()
    val resourceManager = builder.resourceManager
    val chartModel: ChartModelImpl
    private val renderer: TaskRendererImpl2
    private var nextId = 0

    init {
      val projectConfig = UIConfiguration(Color.BLUE, false)
      projectConfig.chartFontOption = DefaultFontOption("foo", FontSpec("Foo", FontSpec.Size.NORMAL), emptyList())
      projectConfig.dpiOption = DefaultIntegerOption("bar", 96)
      chartModel = ChartModelImpl(taskManager, GPTimeUnitStack(), projectConfig)
      chartModel.setBounds(Dimension(1200, 400))
      chartModel.setTopTimeUnit(GPTimeUnitStack.WEEK)
      chartModel.setBottomTimeUnit(GPTimeUnitStack.DAY)
      chartModel.setBottomUnitWidth(dayWidth)
      chartModel.startDate = monday.minusDays(7).toModelDate()
      renderer = TaskRendererImpl2(chartModel)
    }

    fun person(name: String): HumanResource = resourceManager.create(name, nextId++)

    fun task(name: String, start: LocalDate, days: Int): Task =
      taskManager.newTaskBuilder()
        .withName(name)
        .withStartDate(start.toModelDate())
        .withDuration(taskManager.createLength(days.toLong()))
        .build()

    fun boxes(task: Task, style: String? = null): List<Box> {
      chartModel.setVisibleTasks(listOf(task))
      renderer.render()
      val collector = Collector()
      renderer.primitiveContainer.paint(collector)
      renderer.primitiveContainer.layers.forEach { it.paint(collector) }
      return if (style == null) collector.boxes else collector.boxes.filter { it.style == style }
    }

    fun offsets(): List<Offset> = chartModel.defaultUnitOffsets

    fun stripedDays(task: Task): List<LocalDate> {
      val stripes = boxes(task, STYLE_ABSENCE)
      return offsets()
        .filter { offset -> stripes.any { it.left <= offset.startPixels && it.left + it.width >= offset.offsetPixels } }
        .map { it.offsetStart.toModelLocalDate() }
        .distinct()
        .sorted()
    }
  }

  private fun HumanResource.vacation(from: LocalDate, toExclusive: LocalDate) {
    addDaysOff(GanttDaysOff(GanttCalendar.fromLocalDate(from), GanttCalendar.fromLocalDate(toExclusive)))
  }

  private fun HumanResource.vacationOn(day: LocalDate) = vacation(day, day.plusDays(1))

  private fun Task.assign(person: HumanResource, blocking: Boolean = false, noEffort: Boolean = false) {
    assignmentCollection.addAssignment(person).apply {
      load = 100f
      isBlocking = blocking
      isNoEffort = noEffort
    }
  }

  @Test
  fun `only the day of the absence is striped`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacationOn(wednesday) }
    val task = chart.task("t", monday, 5).also { it.assign(a) }

    assertEquals(listOf(wednesday), chart.stripedDays(task), "drawn: ${chart.boxes(task, STYLE_ABSENCE)}")
  }

  @Test
  fun `the stripe lies on the bar and not beside it`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacationOn(wednesday) }
    val task = chart.task("t", monday, 5).also { it.assign(a) }

    val stripe = chart.boxes(task, STYLE_ABSENCE).single()
    val bar = chart.boxes(task).single { it.style in setOf("task", "task.start", "task.startend", "task.end") }
    assertTrue(stripe.left >= bar.left && stripe.left + stripe.width <= bar.left + bar.width, "$stripe vs $bar")
    assertEquals(bar.top, stripe.top)
    assertEquals(bar.height, stripe.height)
  }

  @Test
  fun `a vacation which ends on the first day of the task stripes nothing`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacation(monday.minusDays(2), monday) }
    val task = chart.task("t", monday, 5).also { it.assign(a) }

    assertEquals(emptyList<LocalDate>(), chart.stripedDays(task))
  }

  @Test
  fun `a vacation which begins on the day after the task stripes nothing`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacationOn(saturday.plusDays(2)) }
    val task = chart.task("t", monday, 5).also { it.assign(a) }

    assertEquals(emptyList<LocalDate>(), chart.stripedDays(task))
  }

  @Test
  fun `a vacation up to tuesday stripes monday only`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacation(monday, tuesday) }
    val task = chart.task("t", monday, 5).also { it.assign(a) }

    assertEquals(listOf(monday), chart.stripedDays(task))
  }

  @Test
  fun `a vacation which begins on the last day stripes exactly that day`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacation(friday, friday.plusDays(10)) }
    val task = chart.task("t", monday, 5).also { it.assign(a) }

    assertEquals(listOf(friday), chart.stripedDays(task))
  }

  @Test
  fun `two people away on the same day give one stripe`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacationOn(wednesday) }
    val b = chart.person("B").also { it.vacationOn(wednesday) }
    val task = chart.task("t", monday, 5).also { it.assign(a); it.assign(b) }

    assertEquals(1, chart.boxes(task, STYLE_ABSENCE).size)
  }

  @Test
  fun `two people away on neighbouring days give one rectangle`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacationOn(tuesday) }
    val b = chart.person("B").also { it.vacationOn(wednesday) }
    val task = chart.task("t", monday, 5).also { it.assign(a); it.assign(b) }

    assertEquals(1, chart.boxes(task, STYLE_ABSENCE).size)
    assertEquals(listOf(tuesday, wednesday), chart.stripedDays(task))
  }

  @Test
  fun `an assignment without effort and without blocking is striped too`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacationOn(thursday) }
    val task = chart.task("t", monday, 5).also { it.assign(a, blocking = false, noEffort = true) }

    assertEquals(listOf(thursday), chart.stripedDays(task))
  }

  @Test
  fun `the vacation of a person who is not assigned stripes nothing`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacationOn(wednesday) }
    chart.person("B")
    val task = chart.task("t", monday, 5).also { it.assign(chart.resourceManager.resources[1]) }

    assertEquals(emptyList<LocalDate>(), chart.stripedDays(task))
    assertEquals("A", a.name)
  }

  @Test
  fun `a plan without absences has no stripe at all`() {
    val chart = Chart()
    val a = chart.person("A")
    val task = chart.task("t", monday, 5).also { it.assign(a) }

    assertEquals(0, chart.boxes(task, STYLE_ABSENCE).size)
    assertTrue(chart.boxes(task).isNotEmpty())
  }

  @Test
  fun `a task without assignments has no stripe`() {
    val chart = Chart()
    chart.person("A").vacationOn(wednesday)
    val task = chart.task("t", monday, 5)

    assertEquals(0, chart.boxes(task, STYLE_ABSENCE).size)
  }

  @Test
  fun `the weekend inside a bar is not striped`() {
    val chart = Chart()
    val a = chart.person("A").also { it.vacation(friday, monday.plusDays(7)) }
    val task = chart.task("t", thursday, 4).also { it.assign(a) }

    assertEquals(listOf(friday), chart.stripedDays(task))
  }

  @Test
  fun `neighbouring and overlapping ranges are merged`() {
    val d = { n: Long -> monday.plusDays(n) }
    assertEquals(listOf(AbsenceRun(d(0), d(4))), mergeAbsenceRuns(listOf(d(0) to d(2), d(2) to d(4))))
    assertEquals(listOf(AbsenceRun(d(0), d(5))), mergeAbsenceRuns(listOf(d(0) to d(3), d(2) to d(5))))
    assertEquals(listOf(AbsenceRun(d(0), d(5))), mergeAbsenceRuns(listOf(d(0) to d(5), d(1) to d(2))))
  }

  @Test
  fun `separate ranges stay separate and come in order, empty ranges vanish`() {
    val d = { n: Long -> monday.plusDays(n) }
    assertEquals(
      listOf(AbsenceRun(d(0), d(1)), AbsenceRun(d(3), d(4))),
      mergeAbsenceRuns(listOf(d(3) to d(4), d(0) to d(1), d(2) to d(2)))
    )
  }

  @Test
  fun `the task dialog lists the absent days of the task and writes the last day inclusively`() {
    val chart = Chart()
    val a = chart.person("A").also {
      it.vacation(monday.minusDays(3), tuesday)
      it.vacationOn(thursday)
    }
    val task = chart.task("t", monday, 5).also { it.assign(a) }

    val summary = absenceSummaryForTask(task, a) { it.dayOfMonth.toString() }
    assertEquals("7, 10", summary)
    val wide = chart.person("B").also { it.vacation(tuesday, friday) }
    assertEquals("8 - 10", absenceSummaryForTask(task, wide) { it.dayOfMonth.toString() })
    assertEquals("", absenceSummaryForTask(task, chart.person("C")) { it.dayOfMonth.toString() })
  }
}
