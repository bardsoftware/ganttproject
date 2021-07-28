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
