/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.Painter
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.model.task.RecurrenceRule
import biz.ganttproject.core.model.task.RecurrenceUnit
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.task.recurrence.CollapsedSeriesRows
import biz.ganttproject.task.recurrence.ancestorsToOpen
import biz.ganttproject.task.recurrence.applyRecurrences
import biz.ganttproject.task.recurrence.toModelDate
import biz.ganttproject.task.recurrence.toModelLocalDate
import biz.ganttproject.task.recurrence.planRecurrences
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.chart.AbsenceRun
import net.sourceforge.ganttproject.chart.ChartComparison
import net.sourceforge.ganttproject.chart.ComparisonResult
import net.sourceforge.ganttproject.chart.STYLE_ABSENCE
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.text.DateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/** What a collapsed recurrence series draws: its members as bars in the row of the group. */
class SeriesRowSceneTest {
  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
  }

  @Test
  fun `a collapsed series is drawn as one bar per member in one row`() {
    val scene = scene(Member(AUG_5, 3), Member(AUG_12, 3), Member(AUG_19, 3))
    assertEquals(3, scene.bars.size, "one bar for each member, none for the group itself")
    assertEquals(1, scene.bars.map { it.topY }.distinct().size, "all bars are in the row of the group")
    assertEquals(listOf(AUG_5, AUG_12, AUG_19).map { daysFromChartStart(it) * DAY_WIDTH }, scene.bars.map { it.leftX })
  }

  @Test
  fun `every bar has the progress of its own member`() {
    val scene = scene(Member(AUG_5, 5, 100), Member(AUG_12, 5, 50), Member(AUG_19, 5, 0))
    assertEquals(listOf(5 * DAY_WIDTH, 5 * DAY_WIDTH / 2, 0), scene.progressWidths)
  }

  @Test
  fun `a series with a single member draws a single bar`() {
    val scene = scene(Member(AUG_5, 4, 25))
    assertEquals(1, scene.bars.size)
    assertEquals(listOf(DAY_WIDTH), scene.progressWidths)
  }

  @Test
  fun `overlapping members are drawn over each other in the same row`() {
    val scene = scene(Member(AUG_5, 6), Member(day(2026, Calendar.AUGUST, 8), 6))
    assertEquals(2, scene.bars.size)
    assertEquals(1, scene.bars.map { it.topY }.distinct().size)
    val (first, second) = scene.bars
    assertTrue(second.leftX < first.leftX + first.width, "the second bar starts before the first one ends")
  }

  @Test
  fun `a row which is not a series is drawn as before`() {
    val scene = scene(members = emptyList(), groupStart = AUG_5, groupDays = 5)
    assertEquals(1, scene.bars.size, "the task is drawn as it was")
    assertEquals(5 * DAY_WIDTH, scene.bars.single().width)
  }

  @Test
  fun `every bar has the absence stripes of its own member`() {
    val scene = scene(
      listOf(Member(AUG_5, 5), Member(AUG_12, 5), Member(AUG_19, 5)), AUG_5, 20,
      absence = mapOf(1 to listOf(AbsenceRun(LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 15))))
    )
    val stripe = scene.stripes.single()
    assertEquals(daysFromChartStart(day(2026, Calendar.AUGUST, 13)) * DAY_WIDTH, stripe.leftX)
    assertEquals(2 * DAY_WIDTH, stripe.width)
    assertTrue(scene.bars.count { stripe.leftX >= it.leftX && stripe.leftX < it.leftX + it.width } == 1, "on one bar only")
  }

  @Test
  fun `a member without absence has no stripe`() {
    val scene = scene(
      listOf(Member(AUG_5, 5), Member(AUG_12, 5)), AUG_5, 20,
      absence = mapOf(1 to listOf(AbsenceRun(LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 14))))
    )
    assertEquals(1, scene.stripes.size)
    assertTrue(scene.stripes.single().leftX >= scene.bars[1].leftX, "no stripe on the first bar")
  }

  @Test
  fun `every bar has the comparison band of its own member`() {
    val scene = scene(
      listOf(Member(AUG_5, 3), Member(AUG_12, 3), Member(AUG_19, 3)), AUG_5, 20,
      effort = mapOf(0 to ComparisonResult.MORE, 2 to ComparisonResult.LESS)
    )
    assertEquals(2, scene.bands.size, "the member without a deviation has no band")
    assertEquals(listOf(AUG_5, AUG_19).map { daysFromChartStart(it) * DAY_WIDTH }, scene.bands.map { it.leftX })
    assertTrue(scene.bands[0].hasStyle("later") && scene.bands[1].hasStyle("earlier"))
  }

  @Test
  fun `no band spans the whole row`() {
    val scene = scene(
      listOf(Member(AUG_5, 3), Member(AUG_19, 3)), AUG_5, 20,
      effort = mapOf(-1 to ComparisonResult.MORE)
    )
    assertEquals(0, scene.bands.size, "the group has no deviation of its own to draw")
  }

  private class Member(val start: Date, val days: Int, val completion: Int = 0)

  private class SceneBars(
    val bars: List<Canvas.Rectangle>, val progressWidths: List<Int>,
    val stripes: List<Canvas.Rectangle> = emptyList(), val bands: List<Canvas.Rectangle> = emptyList()
  )

  private fun scene(vararg members: Member): SceneBars = scene(members.toList(), AUG_5, 20)

  private fun scene(
    members: List<Member>, groupStart: Date, groupDays: Int,
    absence: Map<Int, List<AbsenceRun>> = emptyMap(), effort: Map<Int, ComparisonResult> = emptyMap()
  ): SceneBars {
    val calendar = AlwaysWorkingTimeCalendarImpl()
    val group = SingleActivitySceneTask(ROW_ID, groupStart, calendar.shiftDate(groupStart, days(groupDays)), days(groupDays))
    val sceneMembers = members.mapIndexed { i, m ->
      SeriesMemberSceneTask(ROW_ID + 1 + i, m.start, calendar.shiftDate(m.start, days(m.days)), days(m.days), m.completion)
    }
    val stub = InputApiStub(calendar, group, null, ChartComparison.EFFORT)
    val input = object : GanttChartSceneBuilder.InputApi by stub {
      override fun getAbsenceRuns(rowId: Int): List<AbsenceRun> = absence[rowId - ROW_ID - 1] ?: emptyList()
      override fun getEffortComparison(rowId: Int): ComparisonResult = effort[rowId - ROW_ID - 1] ?: ComparisonResult.NO_BAND
      override fun getSeriesMembers(rowId: Int): List<ITaskSceneTask> = if (rowId == ROW_ID) sceneMembers else emptyList()
    }
    val canvas = Canvas()
    GanttChartSceneBuilder(input, canvas).render()
    val rectangles = mutableListOf<Canvas.Rectangle>()
    val collector = object : Painter {
      override fun prePaint() = Unit
      override fun paint(rectangle: Canvas.Rectangle) { rectangles.add(rectangle) }
      override fun paint(line: Canvas.Line) = Unit
      override fun paint(next: Canvas.Text) = Unit
      override fun paint(textGroup: Canvas.TextGroup) = Unit
      override fun paint(rhombus: Canvas.Rhombus) = Unit
    }
    canvas.paint(collector)
    canvas.layers.forEach { it.paint(collector) }
    val progress = rectangles.filter { it.style?.startsWith("task.progress") == true }
    return SceneBars(
      bars = rectangles.filter { it.modelObject is ITaskActivity<*> && it.style != "previousStateTask" }
        .sortedBy { it.leftX },
      stripes = rectangles.filter { it.style == STYLE_ABSENCE }.sortedBy { it.leftX },
      bands = rectangles.filter { it.style == "previousStateTask" }.sortedBy { it.leftX },
      progressWidths = sceneMembers.map { member -> progress.filter { it.modelObject === member }.sumOf { it.width } }
    )
  }

  private fun daysFromChartStart(d: Date): Int = ((d.time - AUG_1.time) / (24 * 3600 * 1000)).toInt()
}

/** What the renderer asks of the members of a series: a bar and a progress, nothing else. */
private class SeriesMemberSceneTask(
  private val rowId: Int, start: Date, end: Date, private val length: TimeDuration, override val completionPercentage: Int
) : TestSceneTask() {
  private val own: List<TaskSceneTaskActivity> = listOf(TaskActivityDataImpl(true, true, 1f, this, start, end, length))
  override fun getRowId(): Int = rowId
  override val color: Color get() = Color.BLUE
  override val end: GanttCalendar = CalendarFactory.createGanttCalendar(end)
  override val activities: List<TaskSceneTaskActivity> get() = own
  override val expand: Boolean get() = true
  override val duration: TimeDuration get() = length
}

private val AUG_1 = day(2026, Calendar.AUGUST, 1)
private val AUG_5 = day(2026, Calendar.AUGUST, 5)
private val AUG_12 = day(2026, Calendar.AUGUST, 12)
private val AUG_19 = day(2026, Calendar.AUGUST, 19)
private const val ROW_ID = 1
private const val DAY_WIDTH = 20

private fun day(year: Int, month: Int, dayOfMonth: Int): Date = GregorianCalendar(year, month, dayOfMonth).time

/** Which tasks a collapsed series puts into the row of its group, on tasks which a real task manager made. */
class SeriesMembersTest {
  private val taskManager: TaskManager = TestSetupHelper.newTaskManagerBuilder().withCalendar(WeekendCalendarImpl()).build()
  private val undo: GPUndoManager = Proxy.newProxyInstance(
    GPUndoManager::class.java.classLoader, arrayOf(GPUndoManager::class.java)) { _, _, args ->
    (args[1] as Runnable).run()
    null
  } as GPUndoManager

  // 2026-11-09 is a Monday.
  private val monday = LocalDate.of(2026, 11, 9)

  private fun series(count: Int, effort: String?): Task {
    val source = taskManager.newTaskBuilder().withName("VAT return").withStartDate(monday.toModelDate()).build()
    source.createMutator().also {
      it.setRecurrenceRule(RecurrenceRule(RecurrenceUnit.WEEK, count = count))
      it.setEffort(effort?.let(::BigDecimal))
      it.commit()
    }
    applyRecurrences(planRecurrences(taskManager), taskManager, undo, "Create")
    return taskManager.taskHierarchy.getContainer(source)
  }

  private fun collapse(task: Task) = task.createMutator().also { it.setExpand(false); it.commit() }

  @Test
  fun `a collapsed group gives its source task and repetitions, earliest first`() {
    val group = series(count = 4, effort = "6")
    collapse(group)
    val members = collapsedSeriesMembers(group, taskManager.taskHierarchy, emptyList())
    assertEquals(4, members.size)
    assertEquals((0L..3L).map { monday.plusWeeks(it) }, members.map { it.start.time.toModelLocalDate() })
  }

  @Test
  fun `a series without effort has members all the same`() {
    val group = series(count = 3, effort = null)
    collapse(group)
    assertEquals(3, collapsedSeriesMembers(group, taskManager.taskHierarchy, emptyList()).size)
  }

  @Test
  fun `a series with one repetition has two members`() {
    val group = series(count = 2, effort = "2")
    collapse(group)
    assertEquals(2, collapsedSeriesMembers(group, taskManager.taskHierarchy, emptyList()).size)
  }

  @Test
  fun `a group whose members have rows is expanded and has nothing to draw in its row`() {
    val group = series(count = 3, effort = "6")
    val rows = taskManager.taskHierarchy.getNestedTasks(group).toList()
    assertTrue(collapsedSeriesMembers(group, taskManager.taskHierarchy, rows).isEmpty())
  }

  @Test
  fun `the row list decides, Task expand is not asked`() {
    val group = series(count = 3, effort = "6")
    val hierarchy = taskManager.taskHierarchy
    val rows = hierarchy.getNestedTasks(group).toList()
    collapse(group)
    assertTrue(collapsedSeriesMembers(group, hierarchy, rows).isEmpty(), "expand=false, but the table shows the members")
    group.createMutator().also { it.setExpand(true); it.commit() }
    assertEquals(3, collapsedSeriesMembers(group, hierarchy, emptyList()).size, "expand=true, but the table hides the members")
    assertTrue(group.expand, "reading the row list does not touch the field")
  }

  @Test
  fun `a press on a member leaves a collapsed series group shut`() {
    val group = series(count = 3, effort = "6")
    val member = taskManager.taskHierarchy.getNestedTasks(group).first()
    val chart = object : CollapsedSeriesRows {
      override fun drawsSeriesInOneRow(task: Task) = task == group
    }
    assertEquals(emptyList<Task>(), ancestorsToOpen(listOf(member), taskManager.taskHierarchy, chart))
  }

  @Test
  fun `any other source still opens the group of the task it selects`() {
    val group = series(count = 3, effort = "6")
    val member = taskManager.taskHierarchy.getNestedTasks(group).first()
    assertEquals(listOf(group), ancestorsToOpen(listOf(member), taskManager.taskHierarchy, "search box"))
    assertEquals(listOf(group), ancestorsToOpen(listOf(member), taskManager.taskHierarchy, null))
  }

  @Test
  fun `the chart keeps shut only the groups whose row it draws`() {
    val group = series(count = 3, effort = "6")
    val member = taskManager.taskHierarchy.getNestedTasks(group).first()
    val chart = object : CollapsedSeriesRows {
      override fun drawsSeriesInOneRow(task: Task) = false
    }
    assertEquals(listOf(group), ancestorsToOpen(listOf(member), taskManager.taskHierarchy, chart))
  }

  @Test
  fun `a collapsed ordinary summary task is not a series`() {
    val summary = taskManager.newTaskBuilder().withName("Phase").build()
    taskManager.newTaskBuilder().withName("Step").withParent(summary).build()
    collapse(summary)
    assertTrue(collapsedSeriesMembers(summary, taskManager.taskHierarchy, emptyList()).isEmpty())
  }
}
