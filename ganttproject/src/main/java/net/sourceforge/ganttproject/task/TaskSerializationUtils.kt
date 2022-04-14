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

import net.sourceforge.ganttproject.util.ColorConvertion
import java.net.URLEncoder


fun Task.externalizedWebLink(): String? {
  if (!webLink.isNullOrBlank() && webLink != "http://") {
    return URLEncoder.encode(webLink, Charsets.UTF_8.name())
  }
  return null
}

// XML CDATA section adds extra line separator on Windows.
// See https://bugs.openjdk.java.net/browse/JDK-8133452.
fun Task.externalizedNotes(): String? = notes?.replace("\\r\\n", "\\n")?.ifBlank { null }

fun TaskImpl.externalizedColor(): String? = if (colorDefined()) ColorConvertion.getColor(color) else null
