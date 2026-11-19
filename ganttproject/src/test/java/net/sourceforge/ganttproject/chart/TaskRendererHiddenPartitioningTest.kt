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

import biz.ganttproject.core.option.DefaultFontOption
import biz.ganttproject.core.option.DefaultIntegerOption
import biz.ganttproject.core.option.FontSpec
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.ganttview.SyncAlgorithm
import biz.ganttproject.ganttview.TaskViewManager
import javafx.scene.control.TreeItem
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.chart.gantt.ITaskSceneTask
import net.sourceforge.ganttproject.gui.UIConfiguration
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Color

/**
 * THE CONNECTION. VerticalPartitioning can be told what the task table hides; this test asks
 * whether TaskRendererImpl2 actually tells it.
 *
 * VerticalPartitioningHiddenTest exercises the partitioning class on its own, with the predicate
 * handed to it by the test. That says nothing about the running chart: as long as
 * TaskRendererImpl2.getVerticalPartitioning leaves the third parameter at its default, the chart
 * behaves exactly as before. So this test goes the other way round -- it never passes a predicate
 * itself. It sets up a real ChartModelImpl, tells it which rows the table shows, and then asks the
 * renderer's own GanttChartSceneBuilder.InputApi for the partitioning it would use when painting.
 *
 * WHAT GOES WRONG WITHOUT THE CONNECTION. A hidden task in the MIDDLE of the document order is
 * skipped between the first and the last visible row and was always right. At the HEAD it lands in
 * aboveViewport, at the TAIL in belowViewport. There
 * GanttChartSceneBuilder.renderTasksAboveAndBelowViewport gives it an invisible rectangle at row -1
 * or row n+1, and DependencySceneBuilder keeps its dependency line, because it only drops a line
 * when BOTH ends are invisible (DependencySceneBuilder.java:153). The line then runs to the edge of
 * the chart, pointing at a task that is not on the chart.
 *
 * THE PRECONDITION this rests on: everything ChartModelImpl is told to show is the FULL row list of
 * the task table, never a scroll window. Checked at all three callers of setVisibleTasks --
 * GanttChartController.paintChart:167, GanttChartController.asPrintChartApi:208 and
 * ChartImageBuilder.kt:54, all fed from TaskTableChartConnector.visibleTasks. The last test in this
 * file pins that precondition so that it cannot be dropped in silence.
 */
class TaskRendererHiddenPartitioningTest {

  /** A plan of six top-level tasks in document order, plus a real chart model over it. */
  private class Fixture {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val chartModel: ChartModelImpl
    private val renderer: TaskRendererImpl2

    init {
      val projectConfig = UIConfiguration(Color.BLACK, false)
      projectConfig.chartFontOption = DefaultFontOption("foo", FontSpec("Foo", FontSpec.Size.NORMAL), emptyList())
      projectConfig.dpiOption = DefaultIntegerOption("bar", 96)
      chartModel = ChartModelImpl(taskManager, GPTimeUnitStack(), projectConfig)
      chartModel.startDate = TestSetupHelper.newMonday().time
      renderer = TaskRendererImpl2(chartModel)
    }

    fun task(name: String, parent: Task = taskManager.rootTask): Task =
      taskManager.newTaskBuilder().withName(name).withParent(parent).build()

    /**
     * Runs the partitioning the way GanttChartSceneBuilder.render does: ask the renderer's own
     * input api for it, then build it over the document order the same api reports.
     */
    fun partition(visibleRows: List<Task>): Pair<List<String>, List<String>> {
      chartModel.setVisibleTasks(visibleRows)
      val api = renderer.GanttChartSceneApi()
      val partitioning = api.verticalPartitioning
      partitioning.build(api.tasksInDocumentOrder)
      return names(partitioning.aboveViewport) to names(partitioning.belowViewport)
    }

    private fun names(tasks: List<ITaskSceneTask>): List<String> =
      tasks.map { taskManager.getTask(it.rowId).name }
  }

  /**
   * THE HEAD AND THE TAIL. This is the failure the whole connection is about.
   *
   * RED against 3af23149b:
   *   org.opentest4j.AssertionFailedError: nothing may stand above the viewport ==>
   *   expected: <[]> but was: <[A]>
   */
  @Test
  fun `a task hidden at the head or at the tail reaches no partition`() {
    val f = Fixture()
    val a = f.task("A")
    val b = f.task("B")
    val c = f.task("C")
    val d = f.task("D")
    val e = f.task("E")
    f.task("F")

    // The table shows B..E; A at the head and F at the tail are hidden by a view or a filter.
    val (above, below) = f.partition(listOf(b, c, d, e))

    assertEquals(emptyList<String>(), above, "nothing may stand above the viewport")
    assertEquals(emptyList<String>(), below, "nothing may stand below the viewport")
    // The counter-check: A and F really are outside the row list, so the assertions above are not
    // green because nothing was hidden in the first place.
    assertEquals(listOf("A", "B", "C", "D", "E", "F"),
      f.taskManager.taskHierarchy.tasksInDocumentOrder.map { it.name },
      "the document order must stay complete -- the chart still needs every task")
    assertEquals(listOf("B", "C", "D", "E"), f.chartModel.visibleTasks.map { it.name })
    assertEquals(a.name, "A")
  }

  /**
   * A HIDDEN SUMMARY TASK AT THE TAIL takes its children along, exactly as the task table does:
   * the tree node is never created, so nothing below it can appear either.
   *
   * RED against 3af23149b:
   *   org.opentest4j.AssertionFailedError: a hidden summary task takes its subtree along ==>
   *   expected: <[]> but was: <[Rohbau, Fundament, Mauern]>
   */
  @Test
  fun `a hidden summary task at the tail takes its children along`() {
    val f = Fixture()
    val ausbau = f.task("Ausbau")
    val abnahme = f.task("Abnahme")
    val rohbau = f.task("Rohbau")
    f.task("Fundament", rohbau)
    f.task("Mauern", rohbau)

    val (above, below) = f.partition(listOf(ausbau, abnahme))

    assertEquals(emptyList<String>(), above)
    assertEquals(emptyList<String>(), below, "a hidden summary task takes its subtree along")
  }

  /**
   * THE WHOLE WAY, from the named view to the partitioning, with the real classes in between:
   * TaskViewManager.viewFxn -> the real SyncAlgorithm -> the rows that survive -> the chart model.
   *
   * The only piece still missing between the two ends is the running JavaFX table, which copies the
   * rows into TaskTableChartConnector.visibleTasks (TaskTable.kt:525-526). That step is read, not
   * seen; everything around it is exercised here.
   *
   * RED against 3af23149b:
   *   org.opentest4j.AssertionFailedError: what the view hides must not reach the chart edge ==>
   *   expected: <[]> but was: <[Rohbau, Fundament, Mauern]>
   */
  @Test
  fun `what a named view hides does not reach the chart edge`() {
    val f = Fixture()
    f.task("Ausbau")
    f.task("Abnahme")
    val rohbau = f.task("Rohbau")
    f.task("Fundament", rohbau)
    f.task("Mauern", rohbau)

    val viewManager = TaskViewManager()
    val view = viewManager.createView("Ohne Rohbau")
    view.hide(rohbau)
    viewManager.addView(view)
    viewManager.activeView = view

    // The production algorithm of the task table decides which rows there are.
    val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()
    val rootItem = TreeItem(f.taskManager.rootTask)
    SyncAlgorithm(
      f.taskManager.taskHierarchy, task2treeItem, rootItem, viewManager.viewFxn, {}, f.taskManager.taskCount
    ).sync()
    val rows = f.taskManager.taskHierarchy.tasksInDocumentOrder.filter { task2treeItem.containsKey(it) }
    assertEquals(listOf("Ausbau", "Abnahme"), rows.map { it.name },
      "counter-check: the view must really have removed the subtree from the tree")

    val (above, below) = f.partition(rows)
    assertEquals(emptyList<String>(), above)
    assertEquals(emptyList<String>(), below, "what the view hides must not reach the chart edge")
  }

  /**
   * SORTING. TaskTable.onSort fills taskTableChartConnector.visibleTasks on its own path
   * (TaskTable.kt:262-278), and it fills it in SORT order, not in document order. Both paths read
   * the same already-pruned tree through getExpandedTasks(), so a hidden task is missing from
   * either -- but the row list the chart gets is then no longer sorted like the document order,
   * and the connection must not depend on that.
   *
   * What this test does NOT claim: that aboveViewport is empty under sorting. VerticalPartitioning
   * takes the FIRST and the LAST element of the row list as the bounds of the viewport, so with a
   * reversed row list the ordinary visible tasks land above the viewport. That is behaviour of the
   * original class, it is the same before and after the connection, and it is deliberately left
   * alone here. The only thing asserted is that the HIDDEN task is in neither partition.
   *
   * RED against 3af23149b:
   *   org.opentest4j.AssertionFailedError: the hidden task must be in no partition, whatever the
   *   row order ==> expected: <[A, B]> but was: <[A, B, C]>
   */
  @Test
  fun `the connection does not depend on the rows being in document order`() {
    val f = Fixture()
    val a = f.task("A")
    val b = f.task("B")
    f.task("C")
    val d = f.task("D")

    // C is hidden; the remaining rows arrive reversed, the way a sorted table hands them over.
    val (above, below) = f.partition(listOf(d, b, a))

    assertEquals(listOf("A", "B"), above,
      "the hidden task must be in no partition, whatever the row order")
    assertEquals(emptyList<String>(), below)
  }

  /**
   * THE PRECONDITION, pinned. With nothing hidden the partitioning must stay empty, and the row
   * list the chart model carries must still be the complete plan. If somebody ever makes
   * setVisibleTasks carry a scroll window instead of the full row list, the set difference in
   * TaskRendererImpl2.getVerticalPartitioning would take the scrolled-away tasks for hidden ones --
   * this test states what is being relied on, it cannot detect that change by itself.
   *
   * This one was already GREEN against 3af23149b; it guards the connection, it does not show the
   * cliff.
   */
  @Test
  fun `with nothing hidden the partitioning stays empty`() {
    val f = Fixture()
    val all = listOf(f.task("A"), f.task("B"), f.task("C"))

    val (above, below) = f.partition(all)

    assertEquals(emptyList<String>(), above)
    assertEquals(emptyList<String>(), below)
  }
}
