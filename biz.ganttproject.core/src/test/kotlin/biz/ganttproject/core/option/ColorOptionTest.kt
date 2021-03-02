/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.ganttproject.core.option

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * @author Dmitry Barashev
 */
class ColorOptionTest {
  @Test
  fun `test isValidColor`() {
    assertFalse(ColorOption.Util.isValidColor(null))
    assertFalse(ColorOption.Util.isValidColor(""))
    assertFalse(ColorOption.Util.isValidColor("zbmcvgjweftweu"))
    assertTrue(ColorOption.Util.isValidColor("#000000"))
    assertTrue(ColorOption.Util.isValidColor("#ffffff"))
    assertFalse(ColorOption.Util.isValidColor("#g0g0g0"))
  }
}
