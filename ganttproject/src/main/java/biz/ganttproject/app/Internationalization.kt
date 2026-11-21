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

import biz.ganttproject.core.option.ValidationException
import javafx.util.StringConverter
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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

fun getNumberFormat(): NumberFormat = NumberFormat.getInstance(ourLocale)

/**
 * Formats a decimal number for editing: uses the separators of the given locale, like the cell display does,
 * but keeps all significant fraction digits, so that committing an unchanged value doesn't round it.
 */
fun formatDecimalForEditing(value: BigDecimal, locale: Locale = ourLocale): String {
  val format = NumberFormat.getInstance(locale)
  format.maximumFractionDigits = maxOf(value.stripTrailingZeros().scale(), format.maximumFractionDigits)
  return format.format(value)
}

/**
 * Parses a decimal number typed by the user, using the decimal and grouping separators of the given locale,
 * e.g. "3,14" in German or Russian. Returns null if the text is blank.
 *
 * @throws ValidationException if the text is not a number in the given locale.
 */
fun parseDecimal(text: String, locale: Locale = ourLocale): BigDecimal? {
  val format = NumberFormat.getInstance(locale)
  var normalized = text.trim()
  if (normalized.isEmpty()) {
    return null
  }
  if (format is DecimalFormat) {
    format.isParseBigDecimal = true
    // Locales such as French or Russian use a non-breaking space as a grouping separator, while
    // users type a regular space.
    val groupingSeparator = format.decimalFormatSymbols.groupingSeparator
    if (Character.isSpaceChar(groupingSeparator)) {
      normalized = normalized.replace(' ', groupingSeparator)
    }
  }
  val position = ParsePosition(0)
  val result = format.parse(normalized, position)
  if (result == null || position.index != normalized.length) {
    throw ValidationException("Can't parse $text as a number")
  }
  return when (result) {
    is BigDecimal -> result
    else -> BigDecimal(result.toString())
  }
}

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
      try {
        LocalDate.parse(str.trim(), shortDateFormat)
      } catch (ignored: DateTimeParseException) {
        // Returning null indicates that the date string could not be parsed.
        null
      }
    }
}
