/*
Copyright 2021 BarD Software s.r.o

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
package biz.ganttproject.core.option

import com.google.common.base.Strings
import com.google.common.base.Supplier
import org.apache.commons.math3.util.Pair
import java.text.DateFormat
import java.text.ParseException
import java.time.Duration
import java.util.*

interface ValueValidator<T> {
  @Throws(ValidationException::class)
  fun parse(text: String): T
}

val voidValidator = object : ValueValidator<Any> {
  override fun parse(text: String): Any = text
}

val integerValidator: ValueValidator<Int> = object : ValueValidator<Int> {
  override fun parse(text: String): Int = try {
    text.toInt()
  } catch (ex: NumberFormatException) {
    throw ValidationException(ex)
  }
}

val doubleValidator: ValueValidator<Double> = object : ValueValidator<Double> {
  override fun parse(text: String): Double = try {
    text.toDouble()
  } catch (ex: NumberFormatException) {
    throw ValidationException(ex)
  }
}

fun createStringDateValidator(dv: DateValidatorType? = null, formats: Supplier<List<DateFormat>>) : ValueValidator<Date> =
  object : ValueValidator<Date> {
    override fun parse(text: String): Date = try {
      if (Strings.isNullOrEmpty(text)) {
        throw ValidationException()
      }
      var parsed: Date? = null
      for (df in formats.get()) {
        parsed = try { df.parse(text) } catch (ex: ParseException) { null }
        if (parsed != null) {
          break;
        }
      }
      if (parsed == null) {
        throw ValidationException("Can't parse value=" + text + "as date")
      }
      dv?.let {
        val validationResult = it(parsed)
        if (!validationResult.first) {
          throw ValidationException(validationResult.second)
        }
      }
      parsed
    } catch (ex: Exception) {
      throw ValidationException(ex)
    }
  }


typealias DateValidatorType = (Date) -> Pair<Boolean, String?>

object DateValidators {
  fun aroundProjectStart(projectStart: Date): DateValidatorType {
    return dateInRange(projectStart, 1000)
  }

  fun dateInRange(center: Date, yearDiff: Int): DateValidatorType = { value: Date ->
    val diff = Duration.between(value.toInstant(), center.toInstant()).abs().dividedBy(Duration.ofDays(365))
    if (diff > yearDiff) {
      Pair.create(false, String.format(
          "Date %s is far away (%d years) from expected date %s. Any mistake?", value, diff, center
        )
      )
    } else {
      Pair.create<Boolean?, String?>(java.lang.Boolean.TRUE, null)
    }
  }

}
