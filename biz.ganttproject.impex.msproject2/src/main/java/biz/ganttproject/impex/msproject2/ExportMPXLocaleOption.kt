/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.impex.msproject2

import biz.ganttproject.app.getCurrentLocale
import biz.ganttproject.core.option.ObservableChoiceOption
import javafx.util.StringConverter
import net.sf.mpxj.mpx.MPXWriter
import java.util.Locale

class ExportMPXLocaleOption: ObservableChoiceOption<Locale>(
  id = "impex.msproject.mpx.language",
  value = findInitialLocale,
  allValues = allMpxLocales,
  converter = stringConverter
)


val allMpxLocales: List<Locale> by lazy {
  MPXWriter.getSupportedLocales().toList().sortedBy { it.getDisplayLanguage(getCurrentLocale()) }
}

val findInitialLocale: Locale by lazy {
  allMpxLocales.find { it == getCurrentLocale() } ?: allMpxLocales.first()
}

val stringConverter: StringConverter<Locale> = object : StringConverter<Locale>() {
  override fun toString(locale: Locale) = locale.getDisplayLanguage(getCurrentLocale())


  override fun fromString(string: String) =
    allMpxLocales.find { it.getDisplayLanguage(getCurrentLocale()) == string } ?: allMpxLocales.first()
}