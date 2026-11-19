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
package biz.ganttproject.ganttview

import biz.ganttproject.core.time.CalendarFactory
import javafx.scene.control.TreeItem
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * THE MOST IMPORTANT PROMISE OF THIS PACKAGE: hiding is display, nothing else.
 *
 * The scheduler must go on seeing the whole plan. The two
 * measurements derived that from the source -- `visibleTasks` does not occur once in
 * the `task/algorithm` package. Derived is not measured, so it is exercised here: the same
 * plan, once with and once without a hidden summary task, must produce the same dates and the same
 * durations.
 *
 * The counter-check is part of every test. Without proof that the view really removes something
 * from the tree, a green run here would only say that nothing happened at all.
 */
class TaskViewCalculationTest {

  init {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : CalendarFactory.LocaleApi {
          override fun getLocale(): Locale = Locale.GERMANY
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMANY)
        })
      }
    }
  }

  private class Plan {
    val project = GanttProjectImpl()
    val tm: TaskManager = project.taskManager
    val rohbau: Task
    val fundament: Task
    val mauern: Task
    val ausbau: Task

    init {
      fun task(name: String, days: Int) = tm.newTaskBuilder()
        .withName(name)
        .withStartDate(java.util.GregorianCalendar(2026, 8, 1).time)
        .withDuration(tm.createLength(days.toLong()))
        .build()

      rohbau = task("Rohbau", 1)
      fundament = task("Fundament", 5)
      mauern = task("Mauern", 10)
      ausbau = task("Ausbau", 4)
      fundament.move(rohbau)
      mauern.move(rohbau)
      // Ausbau follows Mauern. A dependency across the border of the subtree that will be hidden --
      // if hiding ever reached the calculation, this is where it would show.
      tm.dependencyCollection.createDependency(ausbau, mauern)
      tm.algorithmCollection.scheduler.run()
    }

    fun snapshot(): List<String> = tm.tasks.sortedBy { it.taskID }.map {
      "${it.name}|${it.start.toXMLString()}|${it.end.toXMLString()}|${it.duration.length}"
    }

    fun visibleNames(fxn: TaskFilterFxn): Set<String> {
      val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()
      val rootItem = TreeItem(tm.rootTask)
      SyncAlgorithm(tm.taskHierarchy, task2treeItem, rootItem, fxn, {}, tm.taskCount).sync()
      return task2treeItem.keys.filter { it != tm.rootTask }.map { it.name }.toSet()
    }
  }

  /**
   * RED against the state before the build:
   *   expected: <[Ausbau]> but was: <[Rohbau, Fundament, Mauern, Ausbau]>
   * -- the counter-check tripped, because without the views nothing was hidden and the test would
   * have proved nothing.
   */
  @Test
  fun `a hidden summary task does not move a single date`() {
    val plan = Plan()
    val before = plan.snapshot()

    val mgr = TaskViewManager()
    val view = mgr.createView("Nur Ausbau").also { it.hide(plan.rohbau) }
    mgr.addView(view)
    mgr.activeView = view

    // Counter-check first: the view must really take the subtree out of the tree, otherwise
    // everything below is worthless.
    assertEquals(setOf("Ausbau"), plan.visibleNames(mgr.viewFxn),
      "the view hides nothing - the rest of this test would prove nothing")

    // Everything that computes, run again with the view switched on.
    plan.tm.algorithmCollection.scheduler.run()

    assertEquals(before, plan.snapshot(), "hiding changed the plan")
    assertEquals(4, plan.tm.tasks.size, "the model lost a task")
  }

  /**
   * The other direction, so that "nothing changed" cannot be an accident: a REAL change inside the
   * subtree does move the plan, right through the dependency into the part that stays visible. If
   * this test were green as well, the one above would say nothing about hiding.
   *
   * The first attempt used `mauern.delete()` and stayed green -- the snapshot still showed all four
   * tasks. Task.delete() does not take the task out of `taskManager.tasks` here. Extending the
   * duration is the change that really travels.
   */
  @Test
  fun `a real change inside the subtree does move the plan - counter-check`() {
    val plan = Plan()
    val before = plan.snapshot()

    plan.mauern.createMutator().also {
      it.setDuration(plan.tm.createLength(20L))
      it.commit()
    }
    plan.tm.algorithmCollection.scheduler.run()

    assertNotEquals(before, plan.snapshot(),
      "if a change inside the subtree really changes nothing, the test above measures nothing")
  }
}
