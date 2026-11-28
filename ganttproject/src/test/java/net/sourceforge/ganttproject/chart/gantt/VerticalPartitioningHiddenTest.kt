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
package net.sourceforge.ganttproject.chart.gantt

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color

/**
 * The edge cliff at the head and the tail of the document order.
 *
 * WHAT IT IS ABOUT. Everything the task table hides gets no row in the chart, and that alone is
 * right for anything in the MIDDLE of the plan: VerticalPartitioning.build skips it between the
 * first and the last visible row. At the HEAD and at the TAIL it does not: there the hidden task
 * lands in aboveViewport or belowViewport, gets an invisible rectangle at row -1 or row n+1
 * (GanttChartSceneBuilder.renderTasksAboveAndBelowViewport), and DependencySceneBuilder draws a
 * dependency line to it, because it only drops a line when BOTH ends are invisible
 * (DependencySceneBuilder.java:153). The result is a line running to the edge of the chart towards
 * something that is not there.
 *
 * The first measurement derived this from the source and called it the only place where it would
 * like to see a program run. No program is run here, but the partitioning itself is -- with real
 * tasks and the real hierarchy, and both directions of the failure.
 */
class VerticalPartitioningHiddenTest {

  private class SceneTask(private val task: Task) : ITaskSceneTask {
    override fun getRowId(): Int = task.rowId
    override val isCritical = false
    override val isProjectTask = false
    override val hasNestedTasks = false
    override val color: Color get() = Color.BLACK
    override val shape: ShapePaint? = null
    override val notes: String? = null
    override val end: GanttCalendar get() = task.end
    override val activities: List<TaskSceneTaskActivity> = emptyList()
    override val expand: Boolean get() = task.expand
    override val duration: TimeDuration get() = task.duration
    override val completionPercentage = 0
    override fun isMilestone() = false
    override fun getProperty(propertyID: String?): String? = null
    override fun toString() = task.name
  }

  private class Fixture {
    val taskManager: TaskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val tasks: List<Task> = (0 until 6).map {
      taskManager.newTaskBuilder().withId(it).withName("T$it").build()
    }
    val sceneTasks: List<ITaskSceneTask> = tasks.map { SceneTask(it) }
    val areUnrelated: (ITaskSceneTask, ITaskSceneTask) -> Boolean = { t1, t2 ->
      taskManager.taskHierarchy.areUnrelated(taskManager.getTask(t1.rowId), taskManager.getTask(t2.rowId))
    }
  }

  /**
   * RED against the state before the build:
   *   the task hidden at the TAIL got an invisible rectangle and with it a line to the lower edge
   *   expected: <0> but was: <1>
   */
  @Test
  fun `a task hidden at the tail gets no rectangle any more`() {
    val f = Fixture()
    // The table shows T0..T4, T5 is hidden.
    val visible = f.sceneTasks.subList(0, 5)
    val hidden = setOf(f.sceneTasks[5])

    val partitioning = VerticalPartitioning(visible, f.areUnrelated) { hidden.contains(it) }
    partitioning.build(f.sceneTasks)

    assertEquals(0, partitioning.belowViewport.size,
      "the hidden task landed below the viewport and would draw a line to the chart edge: " +
      "${partitioning.belowViewport}")
    assertTrue(partitioning.aboveViewport.isEmpty())
  }

  /**
   * RED against the state before the build:
   *   expected: <0> but was: <1>   -- the same failure at the head of the document order
   */
  @Test
  fun `a task hidden at the head gets no rectangle any more`() {
    val f = Fixture()
    val visible = f.sceneTasks.subList(1, 6)
    val hidden = setOf(f.sceneTasks[0])

    val partitioning = VerticalPartitioning(visible, f.areUnrelated) { hidden.contains(it) }
    partitioning.build(f.sceneTasks)

    assertEquals(0, partitioning.aboveViewport.size,
      "the hidden task landed above the viewport: ${partitioning.aboveViewport}")
    assertTrue(partitioning.belowViewport.isEmpty())
  }

  /**
   * A hidden summary task takes its subtree along here as well -- otherwise a hidden group at the
   * tail would keep its children in belowViewport, and every one of them would draw its line to
   * the edge.
   *
   * RED against the state before the build:
   *   expected: <0> but was: <3>
   */
  @Test
  fun `a hidden summary task takes its subtree out of the partitions`() {
    val f = Fixture()
    // T3 becomes a summary task over T4 and T5, at the tail of the document order.
    f.tasks[4].move(f.tasks[3])
    f.tasks[5].move(f.tasks[3])
    val documentOrder = listOf(0, 1, 2, 3, 4, 5).map { f.sceneTasks[it] }
    val visible = documentOrder.subList(0, 3)
    val hidden = setOf(f.sceneTasks[3])

    val partitioning = VerticalPartitioning(visible, f.areUnrelated) { hidden.contains(it) }
    partitioning.build(documentOrder)

    assertEquals(0, partitioning.belowViewport.size,
      "the hidden group left its children behind: ${partitioning.belowViewport}")
  }

  /**
   * The counter-check that keeps the three tests above honest: WITHOUT the information about what
   * is hidden the class behaves exactly as before -- the task at the tail does land below the
   * viewport. That is the state of the program today, and it is the failure being described.
   *
   * This test would have been GREEN before the build as well. It is listed separately in the
   * report, because it does not check the change but the starting point.
   */
  @Test
  fun `without the information about hiding everything stays as it was`() {
    val f = Fixture()
    val visible = f.sceneTasks.subList(0, 5)

    val partitioning = VerticalPartitioning(visible, f.areUnrelated)
    partitioning.build(f.sceneTasks)

    assertEquals(1, partitioning.belowViewport.size,
      "the old behaviour changed - callers that know nothing about hiding would be affected")
    assertEquals(5, partitioning.belowViewport[0].rowId)
  }

  /**
   * A hidden task in the MIDDLE was never a problem: it falls into the part that build() skips
   * between the first and the last visible row. Measured so that the fix is not credited with
   * something that already worked.
   */
  @Test
  fun `a task hidden in the middle was already correct before`() {
    val f = Fixture()
    val visible = listOf(0, 1, 3, 4, 5).map { f.sceneTasks[it] }

    val withoutKnowledge = VerticalPartitioning(visible, f.areUnrelated)
    withoutKnowledge.build(f.sceneTasks)
    assertTrue(withoutKnowledge.aboveViewport.isEmpty() && withoutKnowledge.belowViewport.isEmpty())

    val withKnowledge = VerticalPartitioning(visible, f.areUnrelated) { it == f.sceneTasks[2] }
    withKnowledge.build(f.sceneTasks)
    assertTrue(withKnowledge.aboveViewport.isEmpty() && withKnowledge.belowViewport.isEmpty())
  }
}
