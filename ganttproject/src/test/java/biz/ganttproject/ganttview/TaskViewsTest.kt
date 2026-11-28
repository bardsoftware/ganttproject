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

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.TreeItem
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Named views for large plans: the grammar they are stored in, and what they do to the task tree.
 *
 * The tree part is deliberately run through the REAL SyncAlgorithm, the same class the task table
 * uses (TaskTable.doSync, :508). A test against a rebuilt filter loop would only prove that the
 * rebuild works.
 */
class TaskViewsTest {

  private fun manager() = TaskViewManager()

  // ------------------------------------------------------------------------------------------
  // The grammar
  // ------------------------------------------------------------------------------------------

  /**
   * RED against the state before the build:
   *   expected: <2> but was: <0>   (encodeTaskViews returned null, decode gave an empty list)
   */
  @Test
  fun `several named views travel in one option value and come back`() {
    val rohbau = NamedTaskView("Rohbau", linkedSetOf("aaa", "bbb"))
    val anlagen = NamedTaskView("Nur Anlagentechnik", linkedSetOf("ccc"))

    val encoded = encodeTaskViews(listOf(rohbau, anlagen))
    val decoded = decodeTaskViews(encoded)

    assertEquals(2, decoded.size, "both views must come back, encoded was:\n$encoded")
    assertEquals("Rohbau", decoded[0].title)
    assertEquals(setOf("aaa", "bbb"), decoded[0].hiddenTaskUids)
    // The name with a blank in it is the reason RecentColorsOption is not the template: it joins
    // with a blank and reads with split("\\s+"), so "Nur Anlagentechnik" would fall into pieces.
    assertEquals("Nur Anlagentechnik", decoded[1].title)
    assertEquals(setOf("ccc"), decoded[1].hiddenTaskUids)
  }

  /**
   * RED against the state before the build:
   *   the name is free user text; without escaping a pasted tab splits one view into two
   *   expected: <A	B
   *   C\D> but was: <>
   */
  @Test
  fun `a view name carrying a tab a newline and a backslash survives`() {
    val evil = "A\tB\nC\\D"
    val decoded = decodeTaskViews(encodeTaskViews(listOf(NamedTaskView(evil, linkedSetOf("uid")))))

    assertEquals(1, decoded.size, "the name broke the grammar and produced ${decoded.size} views")
    assertEquals(evil, decoded[0].title)
    assertEquals(setOf("uid"), decoded[0].hiddenTaskUids)
  }

  /**
   * A project that uses no view must not gain an <option> element. OptionSaver only writes when
   * getPersistentValue() is not null (OptionSaver.java:59).
   *
   * RED against the state before the build: could not be, the function did not exist. Listed
   * separately in the report.
   */
  @Test
  fun `without a view nothing is written`() {
    assertNull(encodeTaskViews(emptyList()))
    assertEquals(emptyList<NamedTaskView>(), decodeTaskViews(null))
    assertEquals(emptyList<NamedTaskView>(), decodeTaskViews(""))
  }

  /**
   * RED against the state before the build:
   *   which view was active must survive a save, exactly as a filter's is-enabled does
   *   expected: <true> but was: <false>
   */
  @Test
  fun `which view was active is remembered`() {
    val a = NamedTaskView("A", linkedSetOf("x"), SimpleBooleanProperty(false))
    val b = NamedTaskView("B", linkedSetOf("y"), SimpleBooleanProperty(true))

    val decoded = decodeTaskViews(encodeTaskViews(listOf(a, b)))

    assertFalse(decoded[0].isEnabledProperty.value)
    assertTrue(decoded[1].isEnabledProperty.value, "the active view was forgotten")
  }

  /**
   * A record that cannot be read is skipped, not guessed. A half-read view would hide tasks that
   * nobody asked to hide, and that is worse than losing the view.
   */
  @Test
  fun `a record that cannot be read is skipped and the others survive`() {
    val broken = "\nkaputt\n1\tGut\tuid1"
    val decoded = decodeTaskViews(broken)

    assertEquals(1, decoded.size)
    assertEquals("Gut", decoded[0].title)
  }

  /** A view without a single hidden task is a legal, empty view -- it must not vanish. */
  @Test
  fun `an empty view keeps its name`() {
    val decoded = decodeTaskViews(encodeTaskViews(listOf(NamedTaskView("Leer", linkedSetOf()))))
    assertEquals(1, decoded.size)
    assertEquals("Leer", decoded[0].title)
    assertEquals(0, decoded[0].hiddenCount)
  }

  // ------------------------------------------------------------------------------------------
  // What a view does to the task tree -- through the real SyncAlgorithm
  // ------------------------------------------------------------------------------------------

  private class Fixture {
    val taskModel = TestSetupHelper.newTaskManagerBuilder().build()
    val rohbau: Task = taskModel.newTaskBuilder().withName("Rohbau").withParent(taskModel.rootTask).build()
    val fundament: Task = taskModel.newTaskBuilder().withName("Fundament").withParent(taskModel.rootTask).build()
    val mauern: Task = taskModel.newTaskBuilder().withName("Mauern").withParent(taskModel.rootTask).build()
    val ausbau: Task = taskModel.newTaskBuilder().withName("Ausbau").withParent(taskModel.rootTask).build()

    init {
      // Rohbau becomes a summary task with two children.
      fundament.move(rohbau)
      mauern.move(rohbau)
    }

    /** Runs the production algorithm and returns the tasks that survived into the tree. */
    fun visibleTasks(fxn: TaskFilterFxn): Set<String> {
      val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()
      val rootItem = TreeItem(taskModel.rootTask)
      SyncAlgorithm(taskModel.taskHierarchy, task2treeItem, rootItem, fxn, {}, taskModel.taskCount).sync()
      return task2treeItem.keys.filter { it != taskModel.rootTask }.map { it.name }.toSet()
    }
  }

  /**
   * The core promise: hiding a SUMMARY task takes its whole subtree along.
   *
   * RED against the state before the build:
   *   expected: <[Ausbau]> but was: <[Rohbau, Fundament, Mauern, Ausbau]>
   */
  @Test
  fun `hiding a summary task takes its whole subtree out of the tree`() {
    val f = Fixture()
    val mgr = manager()
    val view = mgr.createView("Nur Ausbau")
    view.hide(f.rohbau)
    mgr.addView(view)
    mgr.activeView = view

    assertEquals(setOf("Ausbau"), f.visibleTasks(mgr.viewFxn))
  }

  /**
   * THE RETURN PATH. Whatever a view hides comes back, and it does not depend on the table being
   * completely empty -- which is the only situation in which the existing placeholder button
   * appears (TaskTable.kt:512-518).
   *
   * RED against the state before the build:
   *   expected: <[Rohbau, Fundament, Mauern, Ausbau]> but was: <[Ausbau]>
   */
  @Test
  fun `show all brings the hidden subtree back`() {
    val f = Fixture()
    val mgr = manager()
    val view = mgr.createView("Nur Ausbau")
    view.hide(f.rohbau)
    mgr.addView(view)
    mgr.activeView = view
    assertEquals(setOf("Ausbau"), f.visibleTasks(mgr.viewFxn))

    mgr.showAll()

    assertEquals(setOf("Rohbau", "Fundament", "Mauern", "Ausbau"), f.visibleTasks(mgr.viewFxn))
    assertSame(VOID_TASK_VIEW, mgr.activeView)
  }

  /**
   * THE INVERSION, and the reason for it. The list carries what is HIDDEN. A task created after the
   * view was saved is in no list, so it stays visible. The other direction would let it disappear
   * from every named view at once, without a word.
   *
   * RED against the state before the build:
   *   expected: <true> but was: <false>   (nothing was hidden at all, so the test was meaningless)
   */
  @Test
  fun `a task created after the view was saved stays visible`() {
    val f = Fixture()
    val mgr = manager()
    val view = mgr.createView("Rohbau")
    view.hide(f.ausbau)
    mgr.addView(view)
    mgr.activeView = view
    assertFalse(f.visibleTasks(mgr.viewFxn).contains("Ausbau"), "the setup hides nothing")

    val neu = f.taskModel.newTaskBuilder().withName("Dachstuhl").withParent(f.taskModel.rootTask).build()

    assertTrue(f.visibleTasks(mgr.viewFxn).contains("Dachstuhl"),
      "a newly created task fell out of a named view without a word - that is the failure this " +
      "design exists to prevent")
    assertFalse(view.hides(neu))
  }

  /**
   * View and filter side by side, not one on top of the other. This is the decision the
   * measurements asked for: there is exactly one active filter, so a view built AS a filter would
   * switch off "hide completed" and the other way round.
   *
   * RED against the state before the build:
   *   expected: <[Rohbau, Fundament]> but was: <[Rohbau, Fundament, Ausbau]>
   */
  @Test
  fun `a view and a filter both apply at the same time`() {
    val f = Fixture()
    val mgr = manager()
    val view = mgr.createView("Ohne Ausbau")
    view.hide(f.ausbau)
    mgr.addView(view)
    mgr.activeView = view

    // Stands for "hide completed": Mauern is filtered away. Its ancestors stay, the way the
    // built-in filters keep them (TaskTableFilters.refreshCustomFilterResults, :218-236).
    val filterFxn: TaskFilterFxn = { _, child -> child == null || child.name != "Mauern" }
    val both: TaskFilterFxn = { parent, child -> filterFxn(parent, child) && mgr.viewFxn(parent, child) }

    // Counter-check: each condition alone leaves more standing than both together. Without this
    // the test would pass even if one of the two conditions were silently dropped.
    assertEquals(setOf("Rohbau", "Fundament", "Ausbau"), f.visibleTasks(filterFxn))
    assertEquals(setOf("Rohbau", "Fundament", "Mauern"), f.visibleTasks(mgr.viewFxn))
    assertEquals(setOf("Rohbau", "Fundament"), f.visibleTasks(both))
  }

  /**
   * RED against the state before the build:
   *   expected: <[Ausbau]> but was: <[Rohbau, Fundament, Mauern, Ausbau]>
   */
  @Test
  fun `switching between two views changes what is hidden`() {
    val f = Fixture()
    val mgr = manager()
    val nurAusbau = mgr.createView("Nur Ausbau").also { it.hide(f.rohbau) }
    val nurRohbau = mgr.createView("Nur Rohbau").also { it.hide(f.ausbau) }
    mgr.addView(nurAusbau)
    mgr.addView(nurRohbau)

    mgr.activeView = nurAusbau
    assertEquals(setOf("Ausbau"), f.visibleTasks(mgr.viewFxn))

    mgr.activeView = nurRohbau
    assertEquals(setOf("Rohbau", "Fundament", "Mauern"), f.visibleTasks(mgr.viewFxn))
  }

  /**
   * A task may sit in more than one view at once. That is the property a single group value at the
   * task could never have, and it is the reason the views are kept as lists.
   */
  @Test
  fun `one task can be hidden in two views at once`() {
    val f = Fixture()
    val mgr = manager()
    val a = mgr.createView("A").also { it.hide(f.mauern) }
    val b = mgr.createView("B").also { it.hide(f.mauern); it.hide(f.ausbau) }
    mgr.addView(a)
    mgr.addView(b)

    mgr.activeView = a
    assertEquals(setOf("Rohbau", "Fundament", "Ausbau"), f.visibleTasks(mgr.viewFxn))
    mgr.activeView = b
    assertEquals(setOf("Rohbau", "Fundament"), f.visibleTasks(mgr.viewFxn))
  }

  /**
   * Taking the list over from the management dialog: the FIRST ticked entry wins, exactly as
   * TaskFilterManager.importFilters does with `find` (TaskTableFilters.kt:189).
   */
  @Test
  fun `importing views activates the first ticked one`() {
    val mgr = manager()
    val a = NamedTaskView("A", linkedSetOf("x"), SimpleBooleanProperty(false))
    val b = NamedTaskView("B", linkedSetOf("y"), SimpleBooleanProperty(true))
    val c = NamedTaskView("C", linkedSetOf("z"), SimpleBooleanProperty(true))

    mgr.importViews(listOf(a, b, c))

    assertSame(b, mgr.activeView)
    assertEquals(3, mgr.views.size)
    // The manager keeps the tick marks in step with the one active view, so the dialog cannot
    // leave two of them ticked and only one of them working.
    assertFalse(c.isEnabledProperty.value, "a second tick stayed set and would do nothing")
  }

  /** With nothing ticked, nothing is hidden. */
  @Test
  fun `importing views without a tick hides nothing`() {
    val mgr = manager()
    mgr.importViews(listOf(NamedTaskView("A", linkedSetOf("x"))))
    assertSame(VOID_TASK_VIEW, mgr.activeView)
  }

  /**
   * Hiding with no view active must not be a click without an effect: a view is created and
   * activated.
   *
   * RED against the state before the build:
   *   expected: <1> but was: <0>
   */
  @Test
  fun `hiding without an active view creates one`() {
    val f = Fixture()
    val mgr = manager()

    mgr.hideInActiveView(listOf(f.rohbau)) { "Neue Ansicht" }

    assertEquals(1, mgr.views.size)
    assertEquals("Neue Ansicht", mgr.activeView.title)
    assertTrue(mgr.activeView.hides(f.rohbau))
    assertEquals(setOf("Ausbau"), f.visibleTasks(mgr.viewFxn))
  }

  /** Removing the active view is a return path as well: nothing may stay hidden behind it. */
  @Test
  fun `removing the active view shows everything again`() {
    val f = Fixture()
    val mgr = manager()
    val view = mgr.createView("weg").also { it.hide(f.rohbau) }
    mgr.addView(view)
    mgr.activeView = view

    mgr.removeView(view)

    assertSame(VOID_TASK_VIEW, mgr.activeView)
    assertEquals(0, mgr.views.size)
    assertEquals(setOf("Rohbau", "Fundament", "Mauern", "Ausbau"), f.visibleTasks(mgr.viewFxn))
  }

  /**
   * A uid left behind by a deleted task is harmless: it is simply not found. Measured here so that
   * nobody has to trust the reasoning.
   */
  @Test
  fun `a dead uid of a deleted task does no harm`() {
    val f = Fixture()
    val mgr = manager()
    val view = mgr.createView("mit Leiche")
    view.hide(f.ausbau)
    view.hiddenTaskUids.add("00000000000000000000000000000000")
    mgr.addView(view)
    mgr.activeView = view

    assertEquals(setOf("Rohbau", "Fundament", "Mauern"), f.visibleTasks(mgr.viewFxn))
  }
}
