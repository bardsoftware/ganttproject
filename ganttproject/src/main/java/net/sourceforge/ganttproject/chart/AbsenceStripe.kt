/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

This file is part of GanttProject, an open-source project management tool.

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

import biz.ganttproject.core.chart.grid.Offset
import biz.ganttproject.core.chart.grid.OffsetLookup
import biz.ganttproject.task.recurrence.toModelDate
import biz.ganttproject.task.recurrence.toModelLocalDate
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.daysOffRanges
import java.time.LocalDate
import java.util.Date

/** Style name of an absence stripe, read by the painter. */
const val STYLE_ABSENCE = "task.absence"

/** Consecutive days on which somebody assigned to a task is away. The end is exclusive. */
data class AbsenceRun(val start: LocalDate, val endExclusive: LocalDate)

/** A stripe as a half-open pixel span inside one bar rectangle. */
data class StripeSpan(val leftX: Int, val rightX: Int) {
  val width: Int get() = rightX - leftX
}

/**
 * Sorts and merges day ranges into disjoint ascending runs. Touching ranges are merged, so that two people away on
 * the same days, or one vacation right after another, give one stripe.
 */
fun mergeAbsenceRuns(ranges: List<Pair<LocalDate, LocalDate>>): List<AbsenceRun> {
  val sorted = ranges.filter { it.first.isBefore(it.second) }.sortedBy { it.first }
  if (sorted.isEmpty()) {
    return emptyList()
  }
  val merged = mutableListOf<AbsenceRun>()
  var start = sorted[0].first
  var end = sorted[0].second
  sorted.drop(1).forEach { (nextStart, nextEnd) ->
    if (nextStart.isAfter(end)) {
      merged.add(AbsenceRun(start, end))
      start = nextStart
      end = nextEnd
    } else if (nextEnd.isAfter(end)) {
      end = nextEnd
    }
  }
  merged.add(AbsenceRun(start, end))
  return merged
}

/**
 * Days on which anybody assigned to the task is away. Every assignment counts, whether or not it books effort or
 * blocks the task: the stripe only tells that somebody is missing and changes no date. It is not clipped to the task,
 * [absenceStripeSpans] clips it to the activity being drawn.
 */
fun Task.absenceRuns(): List<AbsenceRun> =
  mergeAbsenceRuns(this.assignments.flatMap { it.resource?.daysOffRanges() ?: emptyList() })

/**
 * Pixel spans which the absences [runs] cover inside one bar rectangle.
 *
 * @param segmentStart first day of the activity the rectangle stands for
 * @param segmentEndExclusive the day after its last day
 * @param barLeftX, barRightX the rectangle as drawn; the result is clamped to it
 * @param offsets the day columns the bar was measured with
 */
fun absenceStripeSpans(
  runs: List<AbsenceRun>,
  segmentStart: Date,
  segmentEndExclusive: Date,
  barLeftX: Int,
  barRightX: Int,
  offsets: List<Offset>
): List<StripeSpan> {
  if (runs.isEmpty() || offsets.isEmpty() || barRightX <= barLeftX) {
    return emptyList()
  }
  val lookup = OffsetLookup()
  val spans = mutableListOf<StripeSpan>()
  runs.forEach { run ->
    val from = run.start.toModelDate()
    val toExclusive = run.endExclusive.toModelDate()
    if (from.before(segmentEndExclusive) && toExclusive.after(segmentStart)) {
      val clippedFrom = if (from.before(segmentStart)) segmentStart else from
      val clippedTo = if (toExclusive.after(segmentEndExclusive)) segmentEndExclusive else toExclusive
      val bounds = lookup.getBounds(clippedFrom, clippedTo, offsets)
      val leftX = maxOf(bounds[0], barLeftX)
      val rightX = minOf(bounds[1], barRightX)
      if (rightX > leftX) {
        spans.add(StripeSpan(leftX, rightX))
      }
    }
  }
  return spans
}

/**
 * The days of the task on which the resource is away, for the task dialog: "9 - 11" for a vacation which is stored
 * as [9, 12). Empty if the resource is there throughout.
 */
fun absenceSummaryForTask(task: Task, resource: HumanResource, formatDay: (LocalDate) -> String): String {
  val taskStart = task.start?.time?.toModelLocalDate() ?: return ""
  val taskEndExclusive = task.end?.time?.toModelLocalDate() ?: return ""
  if (!taskStart.isBefore(taskEndExclusive)) {
    return ""
  }
  return mergeAbsenceRuns(resource.daysOffRanges())
    .mapNotNull { run ->
      val from = maxOf(run.start, taskStart)
      val toExclusive = minOf(run.endExclusive, taskEndExclusive)
      if (from.isBefore(toExclusive)) AbsenceRun(from, toExclusive) else null
    }
    .joinToString(", ") { run ->
      val last = run.endExclusive.minusDays(1)
      if (run.start == last) formatDay(run.start) else "${formatDay(run.start)} - ${formatDay(last)}"
    }
}
