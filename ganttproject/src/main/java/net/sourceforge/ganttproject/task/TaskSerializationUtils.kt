/*
Copyright 2022 BarD Software s.r.o., Anastasiia Postnikova

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

package net.sourceforge.ganttproject.task

import net.sourceforge.ganttproject.GanttTask
import net.sourceforge.ganttproject.util.ColorConvertion
import java.math.BigDecimal
import java.net.URLEncoder
import java.sql.Timestamp

fun Task.externalizedPriority(): String? = if (priority != Task.DEFAULT_PRIORITY) priority.persistentValue else null

fun Task.externalizedWebLink(): String? {
  if (!webLink.isNullOrBlank() && webLink != "http://") {
    return URLEncoder.encode(webLink, Charsets.UTF_8.name())
  }
  return null
}

fun Task.externalizedCostManualValue(): BigDecimal? {
  if (!(cost.isCalculated && cost.manualValue == BigDecimal.ZERO)) {
    return cost.manualValue
  }
  return null
}

fun Task.externalizedIsCostCalculated(): Boolean? {
  if (!(cost.isCalculated && cost.manualValue == BigDecimal.ZERO)) {
    return cost.isCalculated
  }
  return null
}

// XML CDATA section adds extra line separator on Windows.
// See https://bugs.openjdk.java.net/browse/JDK-8133452.
fun Task.externalizedNotes(): String? = notes?.replace("\\r\\n", "\\n")?.ifBlank { null }

fun Task.externalizedShape(): String? {
  if (this is GanttTask && shapeDefined() || this !is GanttTask) {
     return shape?.array
  }
  return null
}

fun Task.externalizedColor(): String? {
  if (this is GanttTask && colorDefined() || this !is GanttTask && color != null) {
    return ColorConvertion.getColor(color)
  }
  return null
}

fun Task.externalizedIsMilestone(): Boolean {
  if (this is GanttTask) {
    return isLegacyMilestone
  }
  return isMilestone
}

fun Task.externalizedStartDate(): Timestamp = Timestamp(start.timeInMillis)

fun Task.externalizedEarliestStartDate(): Timestamp? = third?.let { calendar -> Timestamp(calendar.timeInMillis)  }

fun Task.externalizedDurationLength(): Int = duration.length
