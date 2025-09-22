/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject.language

import org.junit.jupiter.api.Test
import org.w3c.util.DateParser
import java.time.LocalDate

class GanttLanguageTest {
  @Test fun `formatting is the same in both SimpleDateFormat and StringConverter`() {
    val lang = GanttLanguage.getInstance()
    val now = LocalDate.now()
    lang.availableLocales.forEach { locale ->
      lang.locale = locale
      val oldFormat = lang.shortDateFormat.format(DateParser.toJavaDate(now))
      val newFormat = lang.shortDateConverter.toString(now)
      assert(oldFormat == newFormat) {
        "Locale $locale has different date formatting: $oldFormat != $newFormat"
      }
      println("locale=$locale format=$newFormat")
    }
  }

}