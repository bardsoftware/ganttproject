/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

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
package net.sourceforge.ganttproject.gui.taskproperties

import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ObservableString
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.option.ValueValidator
import biz.ganttproject.core.option.parseDecimalOrNull
import biz.ganttproject.core.option.validatorI18N
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskMutator
import java.math.BigDecimal

/**
 * Parses the content of a decimal field the way [parseDecimalOrNull] does, which the decimal cells of the tables use too.
 * An empty text means "not set" and gives null.
 * Throws [ValidationException] if the text is not a number or is not within the bounds.
 */
fun parseDecimal(text: String?, min: BigDecimal, isMinExclusive: Boolean = false, max: BigDecimal? = null): BigDecimal? {
  val value = parseDecimalOrNull(text) ?: return null
  val belowMin = if (isMinExclusive) value <= min else value < min
  if (belowMin || (max != null && value > max)) {
    throw ValidationException(validatorI18N("validator.decimal.error.range", arrayOf(text!!.trim())))
  }
  return value
}

fun decimalValidator(min: BigDecimal, isMinExclusive: Boolean = false, max: BigDecimal? = null) =
  ValueValidator<String> { text -> parseDecimal(text, min, isMinExclusive, max); text }

/** Effort in hours, not negative. */
val effortValidator = decimalValidator(BigDecimal.ZERO)

/** Hours per day of a resource: more than zero, at most a day. */
fun parseHoursPerDay(text: String?): BigDecimal? = parseDecimal(text, BigDecimal.ZERO, isMinExclusive = true, max = BigDecimal(24))

val hoursPerDayValidator = ValueValidator<String> { text -> parseHoursPerDay(text); text }

/**
 * Validator of the hours per day field of a resource which had [initial] before the dialog was opened.
 * An empty field is rejected if there was a value, so that it cannot be dropped by accident and fall back to the default
 * without notice. If there was none, the field may stay empty.
 */
fun hoursPerDayValidatorFor(initial: BigDecimal?) = ValueValidator<String> { text ->
  if (initial != null && text.isBlank()) {
    throw ValidationException(validatorI18N("validator.decimal.error.parse", arrayOf(text.trim())))
  }
  parseHoursPerDay(text)
  text
}

/**
 * The effort and the "effort driven" switch of the task properties dialog.
 * The duration which follows from them is calculated when the task mutator is committed.
 */
class TaskEffortFields(private val task: Task) {
  val effortOption = ObservableString("effort", task.effort?.toPlainString(), effortValidator)
  val effortDrivenOption = ObservableBoolean("effortDriven", task.isEffortDriven)

  fun save(mutator: TaskMutator) {
    effortOption.ifChanged {
      val effort = parseDecimal(it, BigDecimal.ZERO)
      val current = task.effort
      // An emptied field of a task without effort is not a change.
      val isSame = if (effort == null || current == null) effort == current else effort.compareTo(current) == 0
      if (!isSame) {
        mutator.setEffort(effort)
      }
    }
    effortDrivenOption.ifChanged(mutator::setEffortDriven)
  }
}
