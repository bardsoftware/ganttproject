/*
Copyright (C) 2026 Dmitry Barashev, GanttProject Team

GanttProject is an opensource project management tool.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package biz.ganttproject.core.option

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.w3c.util.DateParser
import java.time.LocalDate

class ObservableDateOptionTest {
  @Test
  fun testDelegation() {
    val localDate = LocalDate.of(2022, 1, 1)
    val observableDate = ObservableDate("test", localDate)
    val option = ObservableDateOption(observableDate)

    assertEquals(DateParser.toJavaDate(localDate), option.getValue())

    val newLocalDate = LocalDate.of(2022, 2, 2)
    observableDate.value = newLocalDate
    assertEquals(DateParser.toJavaDate(newLocalDate), option.getValue())

    val date3 = DateParser.toJavaDate(LocalDate.of(2022, 3, 3))
    option.setValue(date3)
    assertEquals(LocalDate.of(2022, 3, 3), observableDate.value)
    assertEquals(date3, option.getValue())
  }

  @Test
  fun testPersistence() {
    val localDate = LocalDate.of(2022, 1, 1)
    val option = ObservableDateOption("test", DateParser.toJavaDate(localDate))

    assertEquals("2022-01-01", option.getPersistentValue())

    option.loadPersistentValue("2022-05-05")
    assertEquals(DateParser.toJavaDate(LocalDate.of(2022, 5, 5)), option.getValue())
  }
}
