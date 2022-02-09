/*
 * Copyright (c) 2022 Dmitry Barashev, BarD Software s.r.o.
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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class ValidatorsTest {
  @Test
  fun `test date in range`() {
    val validator = DateValidators.dateInRange(Date(), 5)
    validator(Date.from(LocalDate.now().minusYears(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())).let {
      assertTrue(it.first)
    }
    validator(Date.from(LocalDate.now().minusYears(-2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())).let {
      assertTrue(it.first)
    }
    validator(Date.from(LocalDate.now().minusYears(5).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())).let {
      assertTrue(it.first)
    }
    validator(Date.from(LocalDate.now().minusYears(-5).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())).let {
      assertTrue(it.first)
    }
    validator(Date.from(LocalDate.now().minusYears(6).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())).let {
      assertFalse(it.first)
    }
    validator(Date.from(LocalDate.now().minusYears(-6).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())).let {
      assertFalse(it.first)
    }

  }
}
