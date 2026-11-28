/*
Copyright 2026

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

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.Painter
import biz.ganttproject.core.option.DefaultFontOption
import biz.ganttproject.core.option.DefaultIntegerOption
import biz.ganttproject.core.option.FontSpec
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.chart.gantt.HiddenTaskGap
import net.sourceforge.ganttproject.chart.gantt.STYLE_HIDDEN_GAP
import net.sourceforge.ganttproject.gui.UIConfiguration
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Dimension

/**
 * THE HIDDEN-TASKS MARKER ON THE REAL CHART.
 *
 * `HiddenTasksMarkerTest` proves the arithmetic: which run of hidden tasks is a gap and on which
 * seam it sits. That says nothing about the running chart — as long as nobody asks
 * `hiddenTaskGaps` and nobody creates a rectangle from the answer, a chart with a filter looks
 * exactly as it did before. So this file never computes a gap itself. It builds a real
 * `ChartModelImpl`, tells it which rows the task table shows, RUNS THE RENDERER, and then looks at
 * what is on the canvas.
 *
 * WHAT IT CAN SEE AND WHAT IT CANNOT. It sees every rectangle the scene builder created, its
 * position and its style. It does not see colour — that is `HiddenTasksMarkerImageTest`, which runs
 * the real painter onto a real image — and it does not see the screen, which is in the report.
 */
class HiddenTasksMarkerPainterTest {

  /** Everything a Gantt chart needs to paint itself into nowhere. */
  private class Fixture {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val chartModel: ChartModelImpl
    val renderer: TaskRendererImpl2
    private var day = 0

    init {
      val projectConfig = UIConfiguration(Color.BLACK, false)
      projectConfig.chartFontOption = DefaultFontOption("foo", FontSpec("Foo", FontSpec.Size.NORMAL), emptyList())
      projectConfig.dpiOption = DefaultIntegerOption("bar", 96)
      chartModel = ChartModelImpl(taskManager, GPTimeUnitStack(), projectConfig)
      chartModel.startDate = TestSetupHelper.newMonday().time
      chartModel.setBounds(Dimension(1200, 600))
      chartModel.setBottomUnitWidth(20)
      chartModel.setTopTimeUnit(GPTimeUnitStack.WEEK)
      chartModel.setBottomTimeUnit(GPTimeUnitStack.DAY)
      chartModel.setRowHeight(ROW_HEIGHT)
      renderer = TaskRendererImpl2(chartModel)
    }

    fun task(name: String, parent: Task = taskManager.rootTask): Task {
      val start = TestSetupHelper.newMonday()
      start.add(java.util.Calendar.DAY_OF_MONTH, day)
      day += 7
      return taskManager.newTaskBuilder()
        .withName(name)
        .withParent(parent)
        .withStartDate(start.time)
        .withDuration(taskManager.createLength(3L))
        .build()
    }

    /** Runs the whole renderer and returns every rectangle it left on the base canvas. */
    fun render(visibleRows: List<Task>): List<Canvas.Rectangle> {
      chartModel.setVisibleTasks(visibleRows)
      renderer.render()
      val collected = mutableListOf<Canvas.Rectangle>()
      renderer.primitiveContainer.paint(object : Painter {
        override fun prePaint() {}
        override fun paint(rectangle: Canvas.Rectangle) { collected.add(rectangle) }
        override fun paint(line: Canvas.Line) {}
        override fun paint(next: Canvas.Text) {}
        override fun paint(textGroup: Canvas.TextGroup) {}
        override fun paint(rhombus: Canvas.Rhombus) {}
      })
      return collected
    }

    fun marks(visibleRows: List<Task>): List<Canvas.Rectangle> =
      render(visibleRows).filter { it.style == STYLE_HIDDEN_GAP }

    /** What the renderer's own input api reports — the step between the arithmetic and the drawing. */
    fun reportedGaps(visibleRows: List<Task>): List<HiddenTaskGap> {
      chartModel.setVisibleTasks(visibleRows)
      return renderer.GanttChartSceneApi().hiddenTaskGaps
    }
  }

  /**
   * THE WHOLE WAY: a view hides a task, and a mark appears where it stood.
   *
   * RED before the connection:
   *   org.opentest4j.AssertionFailedError: a hidden task must leave a mark on the chart ==>
   *   expected: <1> but was: <0>
   */
  @Test
  fun `a view that hides a task puts a mark in its place`() {
    val f = Fixture()
    val a = f.task("A")
    val b = f.task("B")
    val c = f.task("C")

    val marks = f.marks(listOf(a, c))

    assertEquals(1, marks.size, "a hidden task must leave a mark on the chart")
    // The seam between the two rows that are drawn, straddled by the mark. The canvas carries the
    // chart's own vertical offset, so the expected value carries it too.
    val deltaY = f.chartModel.chartUIConfiguration.headerHeight - f.chartModel.verticalOffset
    assertEquals(deltaY + ROW_HEIGHT - 2, marks[0].topY,
      "the mark sits on the seam between row 0 and row 1")
    assertTrue(marks[0].width > 0, "and it is wide enough to be seen")
    // The counter-check: B really is off the row list, so the assertion above is not green because
    // nothing was hidden in the first place.
    assertEquals(listOf("A", "B", "C"), f.taskManager.taskHierarchy.tasksInDocumentOrder.map { it.name },
      "the document order must stay complete -- the chart still needs every task")
    assertEquals(listOf("A", "C"), f.chartModel.visibleTasks.map { it.name })
    assertEquals("B", b.name)
  }

  /**
   * ═══ THE GUARD ═══
   *
   * A VIEW THAT HIDES NOTHING DRAWS NO MARK. Green before the change; broken on purpose afterwards,
   * see the report of 08.09.2026.
   */
  @Test
  fun `a chart with nothing hidden carries no mark`() {
    val f = Fixture()
    val all = listOf(f.task("A"), f.task("B"), f.task("C"))

    assertEquals(0, f.marks(all).size)
    assertEquals(emptyList<HiddenTaskGap>(), f.reportedGaps(all))
  }

  /**
   * ═══ THE SECOND GUARD ═══
   *
   * NOTHING ELSE MOVES. The mark is a new rectangle and nothing but a new rectangle: with nothing
   * hidden the renderer must leave the same picture behind as before, and it must not touch the
   * dates while doing so — it is a renderer, and the brief for this package names „weder an den
   * Terminen noch an der Zeichnung" in one breath.
   *
   * THIS CHECK WAS BLIND WHEN IT WAS FIRST WRITTEN and it is worth saying why, because that is the
   * whole point of the exercise. It compared two identical renders with each other and then counted
   * the marks of a THIRD, hidden, one. Break 1 of the report — every task counted as hidden — left
   * it green: both of the identical renders were wrong in the same way, and the third one still had
   * a mark. What was missing is the only line that matters: that the quiet render has NONE.
   */
  @Test
  fun `hiding nothing adds no rectangle at all`() {
    val f = Fixture()
    val a = f.task("A")
    val b = f.task("B")
    val c = f.task("C")
    val datesBefore = listOf(a, b, c).map { "${it.name} ${it.start.time} ${it.end.time}" }

    val quiet = f.render(listOf(a, b, c))
    assertEquals(0, quiet.count { it.style == STYLE_HIDDEN_GAP },
      "a chart that hides nothing must carry no mark at all")
    assertEquals(quiet.size, f.render(listOf(a, b, c)).size, "rendering is repeatable")
    assertEquals(datesBefore, listOf(a, b, c).map { "${it.name} ${it.start.time} ${it.end.time}" },
      "rendering must not move a single date")

    val hidden = f.render(listOf(a, c))
    assertEquals(1, hidden.count { it.style == STYLE_HIDDEN_GAP },
      "one mark, and the counter-check that the run above really was the quiet one")
    assertEquals(datesBefore, listOf(a, b, c).map { "${it.name} ${it.start.time} ${it.end.time}" },
      "and neither must drawing a mark")
  }

  /**
   * THREE HIDDEN TASKS IN A ROW ARE ONE MARK ON THE CHART, not three rectangles stacked on the
   * same seam.
   *
   * RED before the connection:
   *   org.opentest4j.AssertionFailedError: three hidden tasks are one mark ==>
   *   expected: <1> but was: <0>
   */
  @Test
  fun `three hidden tasks in a row make one mark`() {
    val f = Fixture()
    val a = f.task("A")
    f.task("B")
    f.task("C")
    f.task("D")
    val e = f.task("E")

    val marks = f.marks(listOf(a, e))

    assertEquals(1, marks.size, "three hidden tasks are one mark")
    assertEquals(3, f.reportedGaps(listOf(a, e))[0].count, "and it knows there were three")
  }

  /**
   * A COLLAPSED SUMMARY TASK DRAWS NO MARK — on the real chart and not only in the arithmetic.
   * This is the case that decides whether the fork can be used at all: collapsing is what one does
   * with a large plan all day long.
   */
  @Test
  fun `a collapsed summary task draws no mark`() {
    val f = Fixture()
    val rohbau = f.task("Rohbau")
    f.task("Fundament", rohbau)
    f.task("Mauern", rohbau)
    val abnahme = f.task("Abnahme")

    assertEquals(0, f.marks(listOf(rohbau, abnahme)).size,
      "collapsing must not put a hidden-tasks marker on the chart")
  }

  /**
   * THE CLIFF STAYS SHUT. The mark needs the hidden tasks, so the document order must stay
   * complete — and a hidden task must still reach neither partition, which is what the connection
   * of 29.08.2026 is for. Both together in one place, because the hidden-tasks marker is exactly
   * the reason that connection was built the way it was (M2 §8).
   */
  @Test
  fun `the hidden task is still in no partition and still in the document order`() {
    val f = Fixture()
    val a = f.task("A")
    val b = f.task("B")
    val c = f.task("C")

    f.chartModel.setVisibleTasks(listOf(b))
    val api = f.renderer.GanttChartSceneApi()
    val partitioning = api.verticalPartitioning
    partitioning.build(api.tasksInDocumentOrder)

    assertEquals(emptyList<String>(), partitioning.aboveViewport.map { f.taskManager.getTask(it.rowId).name })
    assertEquals(emptyList<String>(), partitioning.belowViewport.map { f.taskManager.getTask(it.rowId).name })
    assertEquals(listOf("A", "B", "C"), api.tasksInDocumentOrder.map { f.taskManager.getTask(it.rowId).name })
    assertEquals(listOf("row=0 count=1", "row=1 count=1"),
      api.hiddenTaskGaps.map { "row=${it.rowIndex} count=${it.count}" },
      "A above and C below are two separate gaps")
    assertEquals("A", a.name)
    assertEquals("C", c.name)
  }

  companion object {
    private const val ROW_HEIGHT = 20
  }
}
