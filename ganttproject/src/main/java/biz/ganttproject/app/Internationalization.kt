/*
Copyright 2019-2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.app

import javafx.util.StringConverter
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private var ourLocale: Locale = Locale.getDefault()

fun setLocale(locale: Locale) {
  ourLocale = locale
  // Touching defaultTranslation ensures that DEFAULT_TRANSLATION_LOCALIZER is initialized
  // (its assignment is a side-effect of the lazy initializer). Without it, locales whose
  // properties file is missing keys fall back to a DummyLocalizer and return null.
  val fallback = defaultTranslation
  ourCurrentTranslation.value = createTranslation(locale) ?: fallback
}

fun getCurrentLocale() = ourLocale

fun String.removeMnemonicsPlaceholder(): String = this.replace("$", "")

fun getNumberFormat(): NumberFormat = NumberFormat.getInstance(ourLocale)

fun createDateConverter(): StringConverter<LocalDate> = LocaleBasedDateConverter(ourLocale)

class LocaleBasedDateConverter(val locale: Locale) : StringConverter<LocalDate>()   {
  private val delegate = FormatterBasedDateConverter(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale))
  override fun toString(date: LocalDate?) = delegate.toString(date)
  override fun fromString(str: String?) = delegate.fromString(str)
}

class FormatterBasedDateConverter(val shortDateFormat: DateTimeFormatter) : StringConverter<LocalDate>() {
  override fun toString(date: LocalDate?): String? =
    date?.let(shortDateFormat::format)


  override fun fromString(str: String?): LocalDate? =
    str?.let {
      LocalDate.parse(str, shortDateFormat)
    }
}

