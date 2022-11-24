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
import javafx.beans.property.StringProperty
import org.apache.commons.math3.util.Pair
import java.text.DateFormat
import java.text.ParseException
import java.time.Duration
import java.util.*

fun interface ValueValidator<T> {
  @Throws(ValidationException::class)
  fun parse(text: String): T
}

val voidValidator = ValueValidator<String> { text -> text }

val integerValidator: ValueValidator<Int> = ValueValidator { text ->
  try {
    text.toInt()
  } catch (ex: NumberFormatException) {
    throw ValidationException(validatorI18N("validator.int.error.parse", arrayOf(text)))
  }
}

val doubleValidator: ValueValidator<Double> = ValueValidator { text ->
  try {
    text.toDouble()
  } catch (ex: NumberFormatException) {
    throw ValidationException(validatorI18N("validator.decimal.error.parse", arrayOf(text)))
  }
}

fun createStringDateValidator(dv: DateValidatorType? = null, formats: Supplier<List<DateFormat>>) : ValueValidator<Date> =
  ValueValidator { text ->
    try {
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
        throw ValidationException("Can't parse value=$text as date")
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

class ValidatedObservable<T>(observableValue: ObservableString, private val validator: ValueValidator<T>) : GPObservable<T?> {

  private var parsedValue: T? = null
  private val watchers: MutableList<ObservableWatcher<T?>> by lazy { mutableListOf() }
  override val value: T? get() = parsedValue
  val validationMessage = ObservableString("")

  init {
    observableValue.addWatcher {sourceEvent -> validate(sourceEvent.newValue ?: "", sourceEvent.trigger) }
    validate(observableValue.value ?: "", null)
  }

  override fun addWatcher(watcher: ObservableWatcher<T?>) {
    watchers.add(watcher)
  }

  internal fun validate(newValue: String, trigger: Any?) {
    val oldValidated = parsedValue
    doValidate(newValue).fold(
      onSuccess = { newValidated ->
        if (newValidated != oldValidated) {
          val evt = ObservableEvent(oldValidated, newValidated, trigger)
          watchers.forEach { it(evt) }
          parsedValue = newValidated
        }
        validationMessage.set(null)
      },
      onFailure = {
        validationMessage.set(it.message ?: "Failed to validate value = $newValue")
      }
    )
  }
  private fun doValidate(value: String): Result<T> = try {
    Result.success(validator.parse(value))
  } catch (ex: ValidationException) {
    Result.failure(ex)
  }

}

fun <T> StringProperty.validated(validator: ValueValidator<T>): ValidatedObservable<T> {
  val textObservable = ObservableString("", this.value)
  return ValidatedObservable(textObservable, validator).also {
    this.addListener { _, _, newValue -> textObservable.set(newValue, this) }
  }
}

var validatorI18N: (key: String, params: Array<Any>) -> String = { key, params -> key }