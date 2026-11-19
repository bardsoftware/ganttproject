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
package net.sourceforge.ganttproject.action

import net.sourceforge.ganttproject.chart.ChartComparison
import net.sourceforge.ganttproject.gui.UIFacade
import java.awt.event.ActionEvent

/**
 * Selects what the band underneath the task bars compares. One action per view; the chart toolbar shows them as a
 * dropdown.
 */
class ChartComparisonAction(
  private val uiFacade: UIFacade,
  val comparison: ChartComparison
) : GPAction(actionId(comparison)) {

  override fun actionPerformed(event: ActionEvent?) {
    val chart = uiFacade.ganttChart
    if (chart.comparison == comparison) {
      return
    }
    chart.comparison = comparison
    uiFacade.refresh()
  }

  companion object {
    /** The actions in dropdown order, which is the order of [ChartComparison]. */
    fun all(uiFacade: UIFacade): List<ChartComparisonAction> =
      ChartComparison.entries.map { ChartComparisonAction(uiFacade, it) }

    fun actionId(comparison: ChartComparison): String = when (comparison) {
      ChartComparison.DATES -> "chart.comparison.dates"
      ChartComparison.EFFORT -> "chart.comparison.effort"
      ChartComparison.DURATIONS -> "chart.comparison.durations"
      ChartComparison.DATES_AND_DURATIONS -> "chart.comparison.datesAndDurations"
    }
  }
}
