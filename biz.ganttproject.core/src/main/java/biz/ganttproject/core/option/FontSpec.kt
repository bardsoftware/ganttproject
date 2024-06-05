/*
Copyright 2014-2024 BarD Software s.r.o

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
package biz.ganttproject.core.option

import java.awt.Font

/**
 * Font specification object, encapsulating font family and size.
 * It is used in option infrastructure which is meant to be portable to other
 * platforms, which is the reason why we don't use java.awt.Font objects.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class FontSpec(val family: String, val size: Size) {
  enum class Size(val factor: Float) {
    SMALLER(0.75f), NORMAL(1.0f), LARGE(1.25f), LARGER(1.5f), HUGE(2.0f)
  }

  fun asString(): String {
    return "$family-$size"
  }

  override fun equals(obj: Any?): Boolean {
    if (obj !is FontSpec) {
      return false
    }
    return family == obj.family && size == obj.size
  }

  override fun hashCode(): Int {
    return family.hashCode()
  }

  override fun toString(): String {
    return asString()
  }

  fun asAwtFont(baseFontSize: Float): Font {
    return asAwtFontOfSize(Math.round(baseFontSize * size.factor))
  }

  fun asAwtFontOfSize(exactFontSize: Int): Font {
    return Font(family, Font.PLAIN, exactFontSize)
  }
}
