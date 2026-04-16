/*
Copyright 2003-2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.core.calendar

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.ObservableEnumerationOption
import biz.ganttproject.core.option.PropertyPaneBuilder
import java.util.*

class ImportCalendarOption @JvmOverloads constructor(initialValue: Values = Values.NO) :
  ObservableEnumerationOption<ImportCalendarOption.Values>(
    "impex.importCalendar", initialValue, Arrays.stream<Values?>(
      Values.entries.toTypedArray()
    ).toList()
  ) {
  enum class Values {
    NO, REPLACE, MERGE;

    override fun toString(): String {
      return "importCalendar_" + name.lowercase(Locale.getDefault())
    }
  }

  init {
    setSelectedValue(initialValue)
  }

  override fun visitPropertyPaneBuilder(builder: PropertyPaneBuilder) {
    builder.dropdown(this.delegate) {
      value2string = { RootLocalizer.formatText("optionValue.importCalendar_${it.name.lowercase()}.label") }
    }
  }
}