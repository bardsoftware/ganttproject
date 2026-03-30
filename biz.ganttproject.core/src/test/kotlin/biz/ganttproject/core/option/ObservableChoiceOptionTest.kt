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

import javafx.util.StringConverter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ObservableChoiceOptionTest {
  private val converter = object : StringConverter<Int>() {
    override fun toString(obj: Int?): String = obj?.toString() ?: ""
    override fun fromString(string: String?): Int? = string?.toIntOrNull()
  }

  @Test
  fun testDelegation() {
    val obsChoice = ObservableChoice("test.id", 1, listOf(1, 2, 3), converter)
    val option = ObservableChoiceOption(obsChoice)

    assertEquals("test.id", option.id)
    assertEquals("1", option.value)
    assertArrayEquals(arrayOf("1", "2", "3"), option.availableValues)

    option.value = "2"
    assertEquals(2, obsChoice.value)
    assertEquals("2", option.value)

    obsChoice.value = 3
    assertEquals("3", option.value)
  }

  @Test
  fun testSelectedValue() {
    val obsChoice = ObservableChoice("test.id", 1, listOf(1, 2, 3), converter)
    val option = ObservableChoiceOption(obsChoice)

    assertEquals(1, option.getSelectedValue())

    option.setSelectedValue(2)
    assertEquals(2, obsChoice.value)
    assertEquals("2", option.value)
  }

  @Test
  fun testChangeListeners() {
    val obsChoice = ObservableChoice("test.id", 1, listOf(1, 2, 3), converter)
    val option = ObservableChoiceOption(obsChoice)
    var changeCount = 0
    option.addChangeValueListener {
      changeCount++
      assertEquals("1", it.oldValue)
      assertEquals("2", it.newValue)
    }

    obsChoice.value = 2
    assertEquals(1, changeCount)
  }
}
