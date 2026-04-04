/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ObservableEnumerationOptionTest {
  enum class TestEnum {
    A, B, C
  }

  @Test
  fun testDelegation() {
    val obsEnum = ObservableEnum("test.id", TestEnum.A, TestEnum.values())
    val option = ObservableEnumerationOption(obsEnum)

    assertEquals("test.id", option.id)
    assertEquals("A", option.value)
    assertArrayEquals(arrayOf("A", "B", "C"), option.availableValues)

    option.value = "B"
    assertEquals(TestEnum.B, obsEnum.value)
    assertEquals("B", option.value)

    obsEnum.value = TestEnum.C
    assertEquals("C", option.value)
  }

  @Test
  fun testSelectedValue() {
    val obsEnum = ObservableEnum("test.id", TestEnum.A, TestEnum.values())
    val option = ObservableEnumerationOption(obsEnum)

    assertEquals(TestEnum.A, option.getSelectedValue())

    option.setSelectedValue(TestEnum.B)
    assertEquals(TestEnum.B, obsEnum.value)
    assertEquals("B", option.value)
  }

  @Test
  fun testChangeListeners() {
    val obsEnum = ObservableEnum("test.id", TestEnum.A, TestEnum.values())
    val option = ObservableEnumerationOption(obsEnum)
    var changeCount = 0
    option.addChangeValueListener {
      changeCount++
      assertEquals("A", it.oldValue)
      assertEquals("B", it.newValue)
    }

    obsEnum.value = TestEnum.B
    assertEquals(1, changeCount)
  }
}
