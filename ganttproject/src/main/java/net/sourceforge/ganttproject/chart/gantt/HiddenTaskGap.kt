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

import biz.ganttproject.app.RootLocalizer
import net.sourceforge.ganttproject.task.Task
import java.util.Date

/**
 * THE HIDDEN-TASKS MARKER — the mark that says „something is missing here".
 *
 * Natalie, 08.09.2026: „wenn man eine ansicht hat die nicht alle Vorgänge zeigt und es eine Lücke
 * gibt das dort der kollisionsbalken liegt und anzeigt das hier eine Lücke ist weil es einen
 * Vorgang gibt den man gerade nicht sieht."
 *
 * WHAT IT IS FOR: honesty of the picture. A named view or a filter removes rows from the task
 * table, and the chart draws exactly the rows the table has ([GanttChartSceneBuilder.renderVisibleTasks]
 * counts them off one by one). What is left is a picture in which a stretch of time simply looks
 * empty, and nothing in it says that the emptiness is a consequence of the view rather than of the
 * plan. This mark is the missing sentence.
 *
 * ═══ WHAT COUNTS AS A GAP, AND WHY IT IS NOT „EVERY TASK THAT IS NOT A ROW" ═══
 *
 * A task can be missing from the row list for two quite different reasons, and only one of them is
 * a lie:
 *
 *  * ITS PARENT IS COLLAPSED. The parent IS on the chart, its summary bar spans the whole subtree,
 *    and the triangle in the table says out loud that there is more underneath. Nothing is being
 *    concealed; a mark here would be noise on a picture that is already telling the truth. Worse,
 *    it would appear every time anybody collapses anything, which is the most common action there
 *    is in this table.
 *  * A VIEW OR A FILTER TOOK IT OUT. Now nothing on the chart stands for it, and the stretch of
 *    time it occupied looks free.
 *
 * TELLING THE TWO APART WITHOUT ASKING THE COLLAPSE STATE. The obvious test is to ask the task
 * whether it is expanded. `Task.expand` CANNOT BE ASKED: M1 §2.3 measured that the JavaFX task
 * table never calls `setExpand`, so the model field is frozen at whatever was loaded from the file
 * while the truth lives in the UI map `TreeCollapseView`, which the chart renderer has no way to
 * reach. A hidden-tasks marker built on `Task.expand` would appear under every summary task the
 * user collapses during the session.
 *
 * SO THE ROW LIST IS ASKED INSTEAD, and it answers without any state at all. For a task that is not
 * a row, find the nearest ancestor that IS a row:
 *
 *  * NO SUCH ANCESTOR — nothing on the chart stands for this task. GAP.
 *  * THERE IS ONE, and it has at least one descendant among the rows: it is therefore drawn
 *    expanded, its subtree is on the chart, and this task was taken out of a sequence that is
 *    otherwise shown. GAP.
 *  * THERE IS ONE, and NOTHING of its subtree is a row: it is collapsed — or the view has removed
 *    every one of its children, which looks exactly the same from here. NO GAP.
 *
 * THE LAST LINE IS THE ONE PLACE THIS GUESSES, and it guesses towards silence: a view that hides
 * every child of a shown summary task gets no mark. That is a miss, not a false alarm, and it is
 * the harmless direction — the summary bar still spans the time, so the picture is not empty where
 * the work is. A mark that appeared under every collapsed task, the other way round, would be a
 * false alarm on the most ordinary action in the program and would teach the user to ignore it.
 *
 * ═══ ONE MARKER PER GAP, NOT ONE PER TASK ═══
 *
 * Three hidden tasks in a row are ONE gap. [hiddenTaskGaps] therefore groups maximal RUNS of
 * consecutive hidden tasks in the document order, and each run becomes one [HiddenTaskGap] spanning
 * from the earliest start to the latest end within it. A hidden summary task and its subtree are
 * consecutive by construction — the document order is depth first — so a hidden subtree is one run
 * as well.
 *
 * [count] counts every task in the run, the children of a hidden summary task included. That is the
 * honest answer to „how many tasks am I not seeing here"; counting only the roots of the run would
 * report 1 for a hidden phase of forty tasks.
 *
 * ═══ THE NAMES ARE NOT IN IT, AND THAT IS A DECISION ═══
 *
 * [HiddenTaskGap] carries a count and two dates and NO names, and the tooltip built from it names
 * nothing either. A view is a deliberate act of leaving something out; a chart that whispers the
 * names undoes exactly the act the user asked for, and it does so in the one place where they
 * cannot switch it off. The marker's job is to stop a false reading of the picture, not to restore
 * the content — whoever wants the content switches the view off, which is one click away. Two dates
 * and a count say „look again with the view off" without being the view.
 *
 * ═══ THE ONE PRECONDITION ═══
 *
 * The row index of a gap is a position in the DOCUMENT ORDER, so it only means anything while the
 * chart draws the rows in that order too. `TaskTable.onSort` fills the row list in SORT order
 * (TaskTable.kt:262-278), and then „the seam between row k-1 and row k" has no relation to the
 * document order at all. [hiddenTaskGaps] detects that case by comparing the row list with the
 * document order projection and returns NO gaps — a mark in the wrong place would be worse than
 * none, because it would name a gap that is not there.
 */
data class HiddenTaskGap(
  /**
   * How many rows stand above this gap. 0 means „above the first row", [rowIndex] == number of rows
   * means „below the last row". The mark is drawn on the seam between row [rowIndex]-1 and row
   * [rowIndex], which is where the tasks were taken out of the sequence.
   */
  val rowIndex: Int,
  /** How many tasks fell out here. Never 0. */
  val count: Int,
  /** The earliest start among them. */
  val start: Date,
  /** The latest end among them, exclusive, exactly as a task bar's end is. */
  val endExclusive: Date
)

/** The style name of a hidden-tasks marker. Read by `StyledPainterImpl` to pick its painter. */
const val STYLE_HIDDEN_GAP = "task.hiddenGap"

/**
 * The height of the mark in pixels, and the height of its two end caps.
 *
 * FIVE, and it has to be small: it is drawn ON THE SEAM between two rows, and the seam is the only
 * part of a row the chart leaves free. In the narrowest row the chart ever draws — no labels, no
 * comparison band, so `rowHeight = fontHeight + 8` — a task bar leaves exactly 4 pixels above and 4
 * below itself (`TaskActivitySceneBuilder.processRegularActivity`). A mark of 5 centred on the seam
 * uses 2 of the one and 3 of the other and touches nothing. In the running program the row is
 * wider still, because `GanttChartController.paintChart` raises it to the table's minimum row
 * height.
 */
const val HIDDEN_GAP_HEIGHT = 5

/**
 * The narrowest a mark may be drawn, in pixels.
 *
 * A hidden MILESTONE has start == end and would be 0 pixels wide, and at the year zoom a hidden
 * week is not much wider. A mark one cannot see is the same as no mark, so the width is raised to
 * this and the arithmetic says so rather than the chart silently dropping it.
 */
const val HIDDEN_GAP_MIN_WIDTH = 5

/**
 * The gaps a view or a filter has torn into [tasksInDocumentOrder].
 *
 * @param tasksInDocumentOrder the complete plan, depth first, WITHOUT the project root -- exactly
 *        what `TaskContainmentHierarchyFacade.getTasksInDocumentOrder` returns.
 * @param rows what the task table shows, in the order the chart draws them.
 * @param container the parent of a task, `null` above the project root. Handed in rather than read
 *        off the task so that this function can be checked without a hierarchy facade.
 */
fun hiddenTaskGaps(
  tasksInDocumentOrder: List<Task>,
  rows: List<Task>,
  container: (Task) -> Task?
): List<HiddenTaskGap> {
  if (tasksInDocumentOrder.isEmpty() || rows.isEmpty()) {
    return emptyList()
  }
  val rowSet = identitySet()
  rowSet.addAll(rows)
  // THE PRECONDITION, checked rather than assumed. See the head comment: a sorted table hands the
  // rows over in sort order, and then a row index taken from the document order points at a seam
  // that has nothing to do with the gap.
  if (tasksInDocumentOrder.filter { rowSet.contains(it) } != rows) {
    return emptyList()
  }
  // Every ancestor of every row. A row that appears in here is drawn EXPANDED -- something below it
  // is on the chart. A row that does not is either collapsed or has had all of its children taken
  // away by the view, and the two cannot be told apart from here; see the head comment for why the
  // ambiguous case is resolved as "collapsed" and what that costs.
  val expandedRows = identitySet()
  rows.forEach { row ->
    var parent = container(row)
    var steps = 0
    while (parent != null && steps < MAX_DEPTH && !expandedRows.contains(parent)) {
      expandedRows.add(parent)
      parent = container(parent)
      steps++
    }
  }

  val gaps = mutableListOf<HiddenTaskGap>()
  var rowIndex = 0
  var runCount = 0
  var runStart: Date? = null
  var runEnd: Date? = null

  fun closeRun() {
    val start = runStart
    val end = runEnd
    if (runCount > 0 && start != null && end != null) {
      gaps.add(HiddenTaskGap(rowIndex, runCount, start, end))
    }
    runCount = 0
    runStart = null
    runEnd = null
  }

  for (task in tasksInDocumentOrder) {
    if (rowSet.contains(task)) {
      closeRun()
      rowIndex++
      continue
    }
    val ancestor = nearestRowAncestor(task, rowSet, container)
    if (ancestor != null && !expandedRows.contains(ancestor)) {
      // Underneath a row that shows nothing of its subtree: collapsed, as far as anything here can
      // tell. The triangle in the table says so and the summary bar spans the time -- no gap.
      continue
    }
    runCount++
    val start = task.start?.time
    val end = task.end?.time
    val currentStart = runStart
    if (start != null && (currentStart == null || start.before(currentStart))) {
      runStart = start
    }
    val currentEnd = runEnd
    if (end != null && (currentEnd == null || end.after(currentEnd))) {
      runEnd = end
    }
  }
  closeRun()
  return gaps
}

/** How far up a hierarchy is walked before it is assumed to be broken. */
private const val MAX_DEPTH = 1000

/** A set that compares by identity. Two different tasks are never equal, whatever `equals` says. */
private fun identitySet(): MutableSet<Task> =
  java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Task, Boolean>())

/**
 * The closest ancestor of [task] that is a row, or null if none is -- then nothing on the chart
 * stands for this task at all. The project root is never a row and its container is null, so the
 * walk always ends.
 */
private fun nearestRowAncestor(
  task: Task,
  rowSet: MutableSet<Task>,
  container: (Task) -> Task?
): Task? {
  var parent = container(task)
  var steps = 0
  while (parent != null && steps < MAX_DEPTH) {
    if (rowSet.contains(parent)) {
      return parent
    }
    parent = container(parent)
    steps++
  }
  return null
}

/**
 * Where one mark is drawn, in the row coordinates of the chart canvas.
 *
 * Separated from the drawing on purpose: the scene builder then
 * has nothing left but to create the rectangle, and every decision about position and size can be
 * checked without a chart, a canvas or a screen.
 *
 * ON THE SEAM, NOT IN A ROW. The gap is a hole in the SEQUENCE of rows, so the mark sits on the
 * line between row [HiddenTaskGap.rowIndex]-1 and row [HiddenTaskGap.rowIndex] -- the very place
 * where the tasks were taken out. That line already exists (`renderVisibleTasks` draws a grey one
 * there), it is the only horizontal strip of a row that never carries a task bar, and a mark there
 * belongs to neither of the two rows it separates, which is right: it belongs to what is between
 * them.
 *
 * THE ALTERNATIVE WAS A NARROW MARK AT THE EDGE OF THE CHART, and it was rejected because it
 * answers only half of Natalie's sentence. „es eine Lücke gibt" is a hole in the picture at a
 * PLACE IN TIME; a mark at the edge says that something is missing somewhere and leaves the reader
 * to guess where. Lying across the days the hidden tasks occupy, the mark says „the emptiness you
 * are looking at, right here, is not empty".
 *
 * THE FIRST ROW IS THE ONE SPECIAL CASE. Its seam is y = 0, and half the mark would reach above the
 * chart area into the timeline header. It is pushed down to y = 0 instead. In the narrowest row the
 * chart draws, that leaves it 4 pixels before the first task bar and it overlaps the top edge of
 * that bar by one pixel; the mark is drawn BEFORE all bars (see `GanttChartSceneBuilder.render`), so
 * the bar wins that pixel and nothing visible is covered.
 *
 * @param leftX, @param rightX the days of the gap, already measured against the chart's own day
 *        columns by the caller's [biz.ganttproject.core.chart.grid.OffsetLookup].
 * @param rowHeight the height of one chart row in pixels.
 */
data class HiddenGapMark(val leftX: Int, val topY: Int, val width: Int, val height: Int)

fun hiddenGapMark(gap: HiddenTaskGap, leftX: Int, rightX: Int, rowHeight: Int): HiddenGapMark =
  HiddenGapMark(
    leftX = leftX,
    topY = maxOf(0, gap.rowIndex * rowHeight - HIDDEN_GAP_HEIGHT / 2),
    width = maxOf(rightX - leftX, HIDDEN_GAP_MIN_WIDTH),
    height = HIDDEN_GAP_HEIGHT
  )

/**
 * What the mark says when the pointer rests on it.
 *
 * THE COUNT AND THE TWO DATES, NO NAMES — see the head comment for why. The count is here and not
 * on the chart itself because there is no room for a letter on the seam: the mark is 5 pixels tall
 * and the smallest font the chart draws is taller than that. A tooltip is the one place where a
 * sentence fits without pushing a row apart.
 *
 * The end is shown inclusively although it is stored exclusively:
 * `[9., 12.)` is what a person reads as „9. – 11.".
 * A gap of a single day is written once and not as a range from itself to itself.
 *
 * @param formatDay how to write one day. Handed in so that this can be checked without a language
 *        being loaded, and so that the chart uses the very date format the rest of the program does.
 */
fun hiddenGapTooltip(gap: HiddenTaskGap, formatDay: (Date) -> String): String {
  val lastDay = Date(gap.endExclusive.time - MILLIS_PER_DAY)
  val period = if (formatDay(gap.start) == formatDay(lastDay)) {
    formatDay(gap.start)
  } else {
    "${formatDay(gap.start)} - ${formatDay(lastDay)}"
  }
  val key = if (gap.count == 1) "taskView.hiddenGap.tooltip.one" else "taskView.hiddenGap.tooltip.many"
  return RootLocalizer.formatText(key, gap.count, period)
}

/**
 * A day in milliseconds. The mark works on chart dates, which are midnights of the local time zone,
 * and the only arithmetic done on them is „one day back" for the inclusive end above. Subtracting
 * 24 hours can land on 23:00 or 01:00 on the two days a year the clocks change; the result is still
 * the right day for a date format that prints no time, which is the only use it has.
 */
private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
