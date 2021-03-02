/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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

import java.awt.Color
import java.util.regex.Pattern

interface ColorOption : GPOption<Color?> {
  object Util {
    private val HEX_COLOR = Pattern.compile("#[0-9abcdefABCDEF]{6}+")
    fun getColor(color: Color): String {
      var res = "#"
      if (color.red <= 15) res += "0"
      res += Integer.toHexString(color.red)
      if (color.green <= 15) res += "0"
      res += Integer.toHexString(color.green)
      if (color.blue <= 15) res += "0"
      res += Integer.toHexString(color.blue)
      return res
    }

    /** parse a string as hew and return the corresponding color.  */
    fun determineColor(hexString: String): Color {
      assert(isValidColor(hexString)) { "Can't parse color $hexString" }
      val r = Integer.valueOf(hexString.substring(1, 3), 16).toInt()
      val g = Integer.valueOf(hexString.substring(3, 5), 16).toInt()
      val b = Integer.valueOf(hexString.substring(5, 7), 16).toInt()
      return Color(r, g, b)
    }

    fun isValidColor(hexString: String?): Boolean {
      return hexString?.let { HEX_COLOR.matcher(it).matches() } ?: false
    }
  }
}
