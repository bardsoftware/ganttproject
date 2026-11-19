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

import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.estimateQuality
import java.math.BigDecimal
import java.util.Date

/**
 * What the band underneath a task bar compares.
 *
 * The dates view is the baseline comparison the chart always had: is the task on schedule? The other views answer
 * different questions which the end date mixes up: did the task take more working hours than first estimated, and did
 * the task get longer, no matter whether it moved.
 */
enum class ChartComparison {
  /** End date now against the end date in the baseline. */
  DATES,

  /** Actual effort against the original effort. Needs no baseline. */
  EFFORT,

  /** Duration now against the duration in the baseline, a mere shift left out. */
  DURATIONS,

  /** The dates comparison in the upper half of the band and the durations comparison in the lower one. */
  DATES_AND_DURATIONS;

  /**
   * Whether the row needs room for a band even if no baseline is shown. The effort view takes its numbers from the
   * task itself, and the durations views draw a neutral band when the baseline is missing.
   */
  val needsBandRoomWithoutBaseline: Boolean get() = this != DATES
}

/** The outcome of a comparison. The painter turns it into one of the baseline colors. */
enum class ComparisonResult {
  /** Nothing to show, no band is drawn. */
  NO_BAND,

  /** Something differs, but it is neither more nor less, or there is nothing to compare with. */
  NEUTRAL,

  /** Later, longer or more hours spent. */
  MORE,

  /** Earlier, shorter or fewer hours spent. */
  LESS;

  /** The style the baseline painter reads, or null for the neutral color. */
  val styleName: String?
    get() = when (this) {
      MORE -> "later"
      LESS -> "earlier"
      else -> null
    }
}

/**
 * The dates view. The color follows the end date; a task which kept its end date but changed its duration still gets
 * a neutral band. The durations are whole units, as stored in the baseline.
 */
fun compareDates(baselineEnd: Date, currentEnd: Date, baselineDuration: Int, currentDuration: Int): ComparisonResult {
  val endComparison = baselineEnd.compareTo(currentEnd)
  return when {
    endComparison == 0 && baselineDuration == currentDuration -> ComparisonResult.NO_BAND
    endComparison < 0 -> ComparisonResult.MORE
    endComparison > 0 -> ComparisonResult.LESS
    else -> ComparisonResult.NEUTRAL
  }
}

/**
 * The durations view. A task which moved but kept its duration shows nothing. Without a baseline the band is neutral,
 * so that a missing baseline does not look like "no change".
 */
fun compareDurations(baselineDuration: Int?, currentDuration: Int): ComparisonResult = when {
  baselineDuration == null -> ComparisonResult.NEUTRAL
  baselineDuration == currentDuration -> ComparisonResult.NO_BAND
  currentDuration > baselineDuration -> ComparisonResult.MORE
  else -> ComparisonResult.LESS
}

/** Both halves of the dates and durations view. */
data class DatesAndDurations(val dates: ComparisonResult, val durations: ComparisonResult)

/**
 * The dates and durations view. Without a baseline each half follows its own view: the dates half has nothing to
 * compare and draws nothing, like the dates view, and the durations half is neutral, like the durations view.
 */
fun compareDatesAndDurations(
  baselineEnd: Date?, currentEnd: Date, baselineDuration: Int?, currentDuration: Int
): DatesAndDurations =
  if (baselineEnd == null || baselineDuration == null) {
    DatesAndDurations(ComparisonResult.NO_BAND, compareDurations(null, currentDuration))
  } else {
    DatesAndDurations(
      compareDates(baselineEnd, currentEnd, baselineDuration, currentDuration),
      compareDurations(baselineDuration, currentDuration)
    )
  }

/** Two efforts which differ by less than a minute are equal. */
private val HOURS_TOLERANCE = BigDecimal.ONE.divide(BigDecimal(60), java.math.MathContext.DECIMAL64)

/**
 * The effort view: the actual effort against the original effort, not against the current one, because a revised
 * estimate would hide the deviation. No original effort draws nothing; an original effort without any actual effort
 * draws a neutral band.
 */
fun compareEffort(originalHours: BigDecimal?, actualHours: BigDecimal?): ComparisonResult = when {
  originalHours == null || originalHours.signum() <= 0 -> ComparisonResult.NO_BAND
  actualHours == null || actualHours.signum() <= 0 -> ComparisonResult.NEUTRAL
  actualHours.subtract(originalHours).abs() < HOURS_TOLERANCE -> ComparisonResult.NO_BAND
  actualHours > originalHours -> ComparisonResult.MORE
  else -> ComparisonResult.LESS
}

/**
 * The effort view for a task, with the numbers of the estimate quality: the actual effort is the sum over the
 * assignments if any of them has one, and a summary task draws nothing.
 */
val Task.effortComparison: ComparisonResult
  get() = estimateQuality?.let { compareEffort(it.originalHours, it.actualHours) } ?: ComparisonResult.NO_BAND
