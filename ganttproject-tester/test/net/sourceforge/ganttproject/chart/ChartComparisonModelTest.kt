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
package net.sourceforge.ganttproject.chart

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.Painter
import biz.ganttproject.core.option.DefaultFontOption
import biz.ganttproject.core.option.DefaultIntegerOption
import biz.ganttproject.core.option.FontSpec
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.gui.UIConfiguration
import net.sourceforge.ganttproject.task.MutableTask
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Dimension
import java.math.BigDecimal

/** The comparison views on real tasks: where the numbers come from, the row height and the rendered band. */
class ChartComparisonModelTest {
  private val builder = TestSetupHelper.newTaskManagerBuilder()
  private val taskManager = builder.build()
  private val alice = builder.resourceManager.create("alice", 1)
  private val bob = builder.resourceManager.create("bob", 2)

  private fun newTask(): Task = taskManager.newTaskBuilder().withName("t")
    .withStartDate(TestSetupHelper.newMonday().time).withDuration(taskManager.createLength(4)).build()

  private fun Task.edit(change: MutableTask.() -> Unit) {
    createMutator().also {
      it.change()
      it.commit()
    }
  }

  @Test
  fun `the effort comparison reads the original and the actual effort of the task`() {
    val task = newTask()
    assertEquals(ComparisonResult.NO_BAND, task.effortComparison, "no original effort")
    task.edit { setEffort(BigDecimal("8")) }
    assertEquals(ComparisonResult.NEUTRAL, task.effortComparison, "estimated, nothing spent yet")
    task.edit { setActualEffort(BigDecimal("10")) }
    assertEquals(ComparisonResult.MORE, task.effortComparison)
    task.edit { setEffort(BigDecimal("12")) }
    assertEquals(ComparisonResult.MORE, task.effortComparison, "a revised estimate does not hide the deviation")
  }

  @Test
  fun `the effort comparison takes the actual effort of the assignments`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("8")) }
    task.edit { setActualEffort(BigDecimal("10")) }
    task.assignmentCollection.addAssignment(alice).actualEffort = BigDecimal("3")
    task.assignmentCollection.addAssignment(bob).actualEffort = BigDecimal("2")
    assertEquals(ComparisonResult.LESS, task.effortComparison, "5 hours over the assignments, the task value is hidden")
  }

  @Test
  fun `a summary task draws no effort band`() {
    val summary = newTask()
    summary.edit { setEffort(BigDecimal("8")) }
    summary.edit { setActualEffort(BigDecimal("10")) }
    taskManager.newTaskBuilder().withName("c").withParent(summary).build()
    assertEquals(ComparisonResult.NO_BAND, summary.effortComparison)
  }

  @Test
  fun `switching to a view which draws without a baseline makes room in the row`() {
    val chartModel = newChartModel(newTask())
    val datesHeight = chartModel.setComparison(ChartComparison.DATES)
    assertEquals(datesHeight + 8, chartModel.setComparison(ChartComparison.EFFORT))
    assertEquals(datesHeight + 8, chartModel.setComparison(ChartComparison.DATES_AND_DURATIONS))
    assertEquals(datesHeight, chartModel.setComparison(ChartComparison.DATES))
    assertEquals(ChartComparison.DATES, chartModel.comparison)
  }

  @Test
  fun `a copy of the chart model keeps the view, so that an export shows it`() {
    val chartModel = newChartModel(newTask())
    chartModel.setComparison(ChartComparison.DURATIONS)
    assertEquals(ChartComparison.DURATIONS, (chartModel.createCopy() as ChartModelImpl).comparison)
  }

  @Test
  fun `the rendered effort view draws a band from the task's numbers`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("8")) }
    task.edit { setActualEffort(BigDecimal("10")) }
    val chartModel = newChartModel(task)
    assertEquals(0, renderBands(chartModel).size, "the dates view without a baseline draws nothing")
    chartModel.setComparison(ChartComparison.EFFORT)
    val bands = renderBands(chartModel)
    assertTrue(bands.isNotEmpty())
    assertTrue(bands.all { it.hasStyle("later") }, "10 hours spent against 8 estimated")
  }

  private fun newChartModel(task: Task): ChartModelImpl {
    val projectConfig = UIConfiguration(Color.BLACK, false)
    projectConfig.setChartFontOption(DefaultFontOption("foo", FontSpec("Foo", FontSpec.Size.HUGE), emptyList()))
    projectConfig.setDpiOption(DefaultIntegerOption("bar", 96))
    return ChartModelImpl(taskManager, GPTimeUnitStack(), projectConfig).also {
      it.setStartDate(TestSetupHelper.newMonday().time)
      it.setVisibleTasks(listOf(task))
      it.setBounds(Dimension(400, 200))
      it.setTopTimeUnit(GPTimeUnitStack.WEEK)
      it.setBottomTimeUnit(GPTimeUnitStack.DAY)
      it.setBottomUnitWidth(20)
    }
  }

  private fun renderBands(chartModel: ChartModelImpl): List<Canvas.Shape> {
    val renderer = TaskRendererImpl2(chartModel)
    chartModel.addRenderer(renderer)
    renderer.render()
    val bands = mutableListOf<Canvas.Shape>()
    val collector = object : Painter {
      override fun prePaint() = Unit
      override fun paint(rectangle: Canvas.Rectangle) = tally(rectangle)
      override fun paint(line: Canvas.Line) = Unit
      override fun paint(next: Canvas.Text) = Unit
      override fun paint(textGroup: Canvas.TextGroup) = Unit
      override fun paint(rhombus: Canvas.Rhombus) = tally(rhombus)
      fun tally(shape: Canvas.Shape) {
        if (shape.style == "previousStateTask") bands.add(shape)
      }
    }
    renderer.primitiveContainer.paint(collector)
    renderer.primitiveContainer.layers.forEach { it.paint(collector) }
    return bands
  }
}
