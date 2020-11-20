/*
Copyright 2020 BarD Software s.r.o

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
package net.sourceforge.ganttproject.calendar

import biz.ganttproject.core.calendar.GPCalendar
import org.eclipse.core.runtime.Platform
import java.io.File
import java.io.IOException

/**
 * Reads calendar files collected from the plugins which provide
 * net.sourceforge.ganttproject.calendar extensions.
 *
 * @author dbarashev@bardsoftware.com
 */
private fun loadCalendarFiles(): List<File> {
  val extensions = Platform.getExtensionRegistry()
      ?.getConfigurationElementsFor("net.sourceforge.ganttproject.calendar")
      ?: return listOf()
  return extensions.mapNotNull { calendarConfig ->
    val path = calendarConfig.getAttribute("path")
    val pattern = calendarConfig.getAttribute("pattern")
    val pluginBundle = Platform.getBundle(calendarConfig.declaringExtension.namespaceIdentifier)
        ?: error("Can't find plugin bundle for extension=" + calendarConfig.name)
    pluginBundle.getResource(path)?.toURI()?.let {
      File(it).listFiles { f: File -> f.name.matches(pattern.toRegex()) }
    } ?: arrayOf()
  }.flatMap { it.asIterable() }
}

/**
 * Parses calendar files collected from the plugins which provide
 * net.sourceforge.ganttproject.calendar extensions.
 *
 * @author dbarashev@bardsoftware.com
 */
fun loadCalendars(): List<GPCalendar> =
    loadCalendarFiles().mapNotNull { file ->
      try {
        GPCalendarProvider.readCalendar(file)
      } catch (ex: IOException) {
        null
      }
    }.toList()
