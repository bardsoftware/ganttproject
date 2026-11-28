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

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.RootLocalizer
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

/**
 * THE HIDDEN-TASKS MARKER — where it belongs and, above all, where it must NOT be.
 *
 * `hiddenTaskGaps` is the whole decision: what is a gap, how many gaps a run of hidden tasks makes,
 * and which row seam a gap sits on. It is a pure function over the document order and the row list,
 * so all of it can be checked here without a chart, a canvas or a screen. The drawing of it is
 * checked in `HiddenTasksMarkerPainterTest` (the connection) and `HiddenTasksMarkerImageTest` (the
 * picture).
 *
 * THE TWO CHECKS THIS FILE EXISTS FOR are `a view that hides nothing has no gap` and
 * `a collapsed summary task is not a gap`. Both are GREEN before the change, and both were made
 * sharp by breaking the finished code on purpose, more than once each — see the report of
 * 08.09.2026, section „Die Brueche".
 */
class HiddenTasksMarkerTest {

  private val savedLocalizer = RootLocalizer

  // The texts live in the localization module; here a key followed by its arguments stands in.
  @BeforeEach
  fun useKeyLocalizer() {
    RootLocalizer = object : DefaultLocalizer() {
      override fun formatTextOrNull(key: String, vararg args: Any): String? = "$key ${args.joinToString(" ")}"
    }
  }

  @AfterEach
  fun restoreLocalizer() {
    RootLocalizer = savedLocalizer
  }

  private class Plan {
    val taskManager: TaskManager = TestSetupHelper.newTaskManagerBuilder().build()
    private var day = 0

    /**
     * A task that starts one day after the previous one, so that every task in a plan has a
     * distinct span and a gap's dates can be told apart from its neighbours'.
     */
    fun task(name: String, parent: Task = taskManager.rootTask, days: Int = 1): Task {
      val start = TestSetupHelper.newMonday()
      start.add(java.util.Calendar.DAY_OF_MONTH, day)
      day += 7
      return taskManager.newTaskBuilder()
        .withName(name)
        .withParent(parent)
        .withStartDate(start.time)
        .withDuration(taskManager.createLength(days.toLong()))
        .build()
    }

    val documentOrder: List<Task> get() = taskManager.taskHierarchy.tasksInDocumentOrder
    val container: (Task) -> Task? get() = { taskManager.taskHierarchy.getContainer(it) }

    fun gaps(rows: List<Task>): List<HiddenTaskGap> = hiddenTaskGaps(documentOrder, rows, container)

    /** A short, readable form of a gap list for an assertion message. */
    fun describe(gaps: List<HiddenTaskGap>): List<String> =
      gaps.map { "row=${it.rowIndex} count=${it.count}" }
  }

  /**
   * ONE HIDDEN TASK IN THE MIDDLE. The plainest case there is, and the one Natalie described.
   *
   * RED against the unbuilt arithmetic:
   *   org.opentest4j.AssertionFailedError: a hidden task must leave a gap behind ==>
   *   expected: <[row=1 count=1]> but was: <[]>
   */
  @Test
  fun `a hidden task leaves one gap at the seam it was taken from`() {
    val p = Plan()
    val a = p.task("A")
    val b = p.task("B")
    val c = p.task("C")

    val gaps = p.gaps(listOf(a, c))

    assertEquals(listOf("row=1 count=1"), p.describe(gaps), "a hidden task must leave a gap behind")
    assertEquals(b.start.time, gaps[0].start, "the gap must start where the hidden task starts")
    assertEquals(b.end.time, gaps[0].endExclusive, "and end where it ends")
  }

  /**
   * THREE IN A ROW ARE ONE GAP, NOT THREE. Natalie's own wording of it, and the reason
   * [hiddenTaskGaps] groups runs instead of reporting tasks.
   *
   * RED against the unbuilt arithmetic:
   *   org.opentest4j.AssertionFailedError: three consecutive hidden tasks are ONE gap ==>
   *   expected: <[row=1 count=3]> but was: <[]>
   */
  @Test
  fun `three consecutive hidden tasks make one gap`() {
    val p = Plan()
    val a = p.task("A")
    val b = p.task("B")
    p.task("C")
    val d = p.task("D")
    val e = p.task("E")

    val gaps = p.gaps(listOf(a, e))

    assertEquals(listOf("row=1 count=3"), p.describe(gaps), "three consecutive hidden tasks are ONE gap")
    assertEquals(b.start.time, gaps[0].start, "the gap spans from the first hidden task ...")
    assertEquals(d.end.time, gaps[0].endExclusive, "... to the last")
  }

  /**
   * TWO SEPARATE GAPS STAY TWO. The counter-check to the one above: grouping must not swallow a
   * visible row that stands between two hidden ones.
   *
   * RED against the unbuilt arithmetic:
   *   org.opentest4j.AssertionFailedError: expected: <[row=1 count=1, row=2 count=1]> but was: <[]>
   */
  @Test
  fun `a visible row between two hidden ones separates the gaps`() {
    val p = Plan()
    val a = p.task("A")
    p.task("B")
    val c = p.task("C")
    p.task("D")
    val e = p.task("E")

    assertEquals(listOf("row=1 count=1", "row=2 count=1"), p.describe(p.gaps(listOf(a, c, e))))
  }

  /**
   * THE HEAD AND THE TAIL. Row index 0 means „above the first row", row index n means „below the
   * last one". Both are real positions, and both were wrong in an earlier draft that started
   * counting at the first visible row.
   *
   * RED against the unbuilt arithmetic:
   *   org.opentest4j.AssertionFailedError: expected: <[row=0 count=1, row=2 count=1]> but was: <[]>
   */
  @Test
  fun `a gap at the head sits above the first row and one at the tail below the last`() {
    val p = Plan()
    p.task("A")
    val b = p.task("B")
    val c = p.task("C")
    p.task("D")

    assertEquals(listOf("row=0 count=1", "row=2 count=1"), p.describe(p.gaps(listOf(b, c))))
  }

  /**
   * A HIDDEN SUMMARY TASK TAKES ITS SUBTREE ALONG and the whole subtree is ONE gap, counted in
   * full. Counting only the summary task would report „1 hidden" for a phase of forty.
   *
   * RED against the unbuilt arithmetic:
   *   org.opentest4j.AssertionFailedError: a hidden summary task and its children are one gap ==>
   *   expected: <[row=1 count=3]> but was: <[]>
   */
  @Test
  fun `a hidden summary task and its subtree are one gap`() {
    val p = Plan()
    val ausbau = p.task("Ausbau")
    val rohbau = p.task("Rohbau")
    p.task("Fundament", rohbau)
    p.task("Mauern", rohbau)
    val abnahme = p.task("Abnahme")

    assertEquals(listOf("row=1 count=3"), p.describe(p.gaps(listOf(ausbau, abnahme))),
      "a hidden summary task and its children are one gap")
  }

  /**
   * ═══ THE FIRST GUARD ═══
   *
   * A VIEW THAT HIDES NOTHING SHOWS NO MARKER. Without this the mark would appear on every chart,
   * and an ever-present warning is the same as none.
   *
   * GREEN before the change — it has to be, it says that nothing happens. What makes it worth
   * having is that it was broken on purpose four times after the code was finished and failed each
   * time; the report of 08.09.2026 lists the four breaks and their output.
   */
  @Test
  fun `a view that hides nothing has no gap`() {
    val p = Plan()
    val all = listOf(p.task("A"), p.task("B"), p.task("C"))

    assertEquals(emptyList<String>(), p.describe(p.gaps(all)))
  }

  /**
   * The same, with a hierarchy: a plan in which every task is a row, summary tasks included, must
   * be silent as well. A rule that treated „is not a leaf" as „hides something" would pass the
   * check above and fail here.
   */
  @Test
  fun `a fully shown plan with summary tasks has no gap`() {
    val p = Plan()
    val rohbau = p.task("Rohbau")
    val fundament = p.task("Fundament", rohbau)
    val mauern = p.task("Mauern", rohbau)
    val abnahme = p.task("Abnahme")

    assertEquals(emptyList<String>(), p.describe(p.gaps(listOf(rohbau, fundament, mauern, abnahme))))
  }

  /**
   * ═══ THE SECOND GUARD ═══
   *
   * A COLLAPSED SUMMARY TASK IS NOT A GAP. Collapsing is the most ordinary action in the task
   * table, the triangle says out loud that there is more underneath, and the summary bar spans the
   * children's time. A mark here would fire on nearly every chart.
   *
   * This is also the check that decides the shape of the whole rule: `Task.expand` is frozen after
   * loading (M1 §2.3) and cannot answer it, so the answer is taken from the row list — a row with
   * no descendant among the rows is drawn collapsed.
   *
   * GREEN before the change. Broken on purpose afterwards; see the report.
   */
  @Test
  fun `a collapsed summary task is not a gap`() {
    val p = Plan()
    val rohbau = p.task("Rohbau")
    p.task("Fundament", rohbau)
    p.task("Mauern", rohbau)
    val abnahme = p.task("Abnahme")

    // The table shows Rohbau and Abnahme; Fundament and Mauern are missing because Rohbau is
    // collapsed, not because anything hides them.
    assertEquals(emptyList<String>(), p.describe(p.gaps(listOf(rohbau, abnahme))),
      "collapsing a summary task must not raise a hidden-tasks marker")
  }

  /**
   * ═══ THE SECOND GUARD, DEEPER ═══
   *
   * A COLLAPSED SUBTREE TWO LEVELS DEEP IS NOT A GAP EITHER, and this check exists because the one
   * above could not see the third break of the report: „only the DIRECT parent is looked at". With
   * a subtree one level deep the direct parent IS the collapsed row, so a rule that never walks
   * upwards gives the right answer for the wrong reason and all twenty-six checks stayed green.
   * Bohrung's parent is Fundament, which is not a row either; only walking up as far as Rohbau
   * finds the row that is drawn collapsed.
   */
  @Test
  fun `a collapsed subtree two levels deep is not a gap`() {
    val p = Plan()
    val rohbau = p.task("Rohbau")
    val fundament = p.task("Fundament", rohbau)
    p.task("Bohrung", fundament)
    val abnahme = p.task("Abnahme")

    assertEquals(emptyList<String>(), p.describe(p.gaps(listOf(rohbau, abnahme))),
      "a collapsed subtree must stay silent however deep it is")
  }

  /**
   * A CHILD HIDDEN UNDER AN EXPANDED PARENT IS A GAP. The other half of the rule above, and the
   * check that keeps it from being satisfied by simply never reporting anything below a summary
   * task: Rohbau is on the chart WITH children, so the child that is missing was taken out.
   *
   * RED against the unbuilt arithmetic:
   *   org.opentest4j.AssertionFailedError: a child taken out of a shown subtree is a gap ==>
   *   expected: <[row=2 count=1]> but was: <[]>
   */
  @Test
  fun `a child hidden under an expanded parent is a gap`() {
    val p = Plan()
    val rohbau = p.task("Rohbau")
    val fundament = p.task("Fundament", rohbau)
    p.task("Mauern", rohbau)
    val dach = p.task("Dach", rohbau)

    assertEquals(listOf("row=2 count=1"), p.describe(p.gaps(listOf(rohbau, fundament, dach))),
      "a child taken out of a shown subtree is a gap")
  }

  /**
   * THE PRECONDITION, pinned. A sorted table hands its rows over in sort order
   * (TaskTable.onSort, TaskTable.kt:262-278), and then a row index taken from the document order
   * points at a seam that has nothing to do with the gap. No mark is better than a misplaced one.
   */
  @Test
  fun `no gaps are reported when the rows are not in document order`() {
    val p = Plan()
    val a = p.task("A")
    p.task("B")
    val c = p.task("C")
    val d = p.task("D")

    assertTrue(p.gaps(listOf(a, c, d)).isNotEmpty(), "counter-check: in document order there IS a gap")
    assertEquals(emptyList<String>(), p.describe(p.gaps(listOf(d, c, a))),
      "a sorted row list must not produce a mark at a seam it cannot name")
  }

  /**
   * ═══ THE GEOMETRY ═══
   *
   * ON THE SEAM. Row 2's mark straddles y = 2 * rowHeight.
   *
   * RED against the unbuilt geometry:
   *   org.opentest4j.AssertionFailedError: the mark straddles the seam ==>
   *   expected: <38> but was: <0>
   */
  @Test
  fun `the mark straddles the seam between the two rows`() {
    val mark = hiddenGapMark(HiddenTaskGap(2, 1, Date(0), Date(0)), leftX = 100, rightX = 160, rowHeight = 20)

    assertEquals(38, mark.topY, "the mark straddles the seam")
    assertEquals(HIDDEN_GAP_HEIGHT, mark.height)
    assertEquals(100, mark.leftX)
    assertEquals(60, mark.width)
  }

  /**
   * THE FIRST ROW cannot straddle its seam — half of the mark would land in the timeline header.
   *
   * RED against the unbuilt geometry:
   *   org.opentest4j.AssertionFailedError: the mark may not reach into the header ==>
   *   expected: <0> but was: <0>   (the stub happened to agree; the width below did not)
   */
  @Test
  fun `a gap above the first row is pushed below the top edge`() {
    val mark = hiddenGapMark(HiddenTaskGap(0, 1, Date(0), Date(0)), leftX = 10, rightX = 40, rowHeight = 20)

    assertEquals(0, mark.topY, "the mark may not reach into the header")
    assertEquals(30, mark.width)
  }

  /**
   * A HIDDEN MILESTONE has start == end and would be nought pixels wide. A mark nobody can see is
   * the same as no mark at all.
   *
   * RED against the unbuilt geometry:
   *   org.opentest4j.AssertionFailedError: a mark is never narrower than this ==>
   *   expected: <5> but was: <0>
   */
  @Test
  fun `a mark is never narrower than the minimum`() {
    val mark = hiddenGapMark(HiddenTaskGap(1, 1, Date(0), Date(0)), leftX = 70, rightX = 70, rowHeight = 20)

    assertEquals(HIDDEN_GAP_MIN_WIDTH, mark.width, "a mark is never narrower than this")
    assertEquals(70, mark.leftX)
  }

  /**
   * ═══ WHAT IT SAYS ═══
   *
   * The count and the two days, and NOTHING ELSE. The names of the hidden tasks are what the view
   * was asked to leave out; see the head comment of `HiddenTaskGap.kt`.
   *
   * RED against the missing texts:
   *   org.opentest4j.AssertionFailedError: expected: <3 ausgeblendete Vorgänge (1 - 5)> but was:
   *   <taskView.hiddenGap.tooltip.many>
   */
  @Test
  fun `the tooltip names a count and a period and no task`() {
    val gap = HiddenTaskGap(1, 3, Date(0), Date(4L * 24 * 60 * 60 * 1000))
    val text = hiddenGapTooltip(gap) { d -> (d.time / (24L * 60 * 60 * 1000) + 1).toString() }

    assertTrue(text.startsWith("taskView.hiddenGap.tooltip.many "), "plural key: $text")
    assertTrue(text.contains("3"), "the count is in it: $text")
    assertTrue(text.contains("1") && text.contains("4"), "the first and the last day are in it: $text")
  }

  /** One hidden task is written „1 Vorgang", and a single day is written once and not as a range. */
  @Test
  fun `a single hidden task on a single day reads as one and not as a range`() {
    val gap = HiddenTaskGap(1, 1, Date(0), Date(24L * 60 * 60 * 1000))
    val text = hiddenGapTooltip(gap) { "Montag" }

    assertTrue(text.startsWith("taskView.hiddenGap.tooltip.one "), "singular key: $text")
    assertEquals(1, Regex("Montag").findAll(text).count(), "one day is named once: $text")
  }
}
