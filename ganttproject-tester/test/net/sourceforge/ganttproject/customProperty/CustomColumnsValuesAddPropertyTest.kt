/*
 * Copyright 2026 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package net.sourceforge.ganttproject.customProperty

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.customproperty.CustomColumnsManager
import biz.ganttproject.customproperty.CustomColumnsValues
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyValueEventStub
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CustomColumnsValuesAddPropertyTest {
  @Test
  fun `adds a valid value and stores it`() {
    val manager = CustomColumnsManager()
    val def = manager.createDefinition(CustomPropertyClass.TEXT, "foo")
    val values = CustomColumnsValues(manager) {}

    val result = values.addCustomProperty(def, "bar")

    assertEquals("bar", result?.value)
    assertEquals(def, result?.definition)
    assertEquals("bar", values.getValue(def))
    assertTrue(values.hasOwnValue(def))
  }

  @Test
  fun `converts value according to the property type`() {
    val manager = CustomColumnsManager()
    val def = manager.createDefinition(CustomPropertyClass.INTEGER, "foo")
    val values = CustomColumnsValues(manager) {}

    val result = values.addCustomProperty(def, "42")

    assertEquals(42, result?.value)
    assertEquals(42, values.getValue(def))
  }

  @Test
  fun `converts value for a date typed column`() {
    val manager = CustomColumnsManager()
    val def = manager.createDefinition(CustomPropertyClass.DATE, "foo")
    val values = CustomColumnsValues(manager) {}

    val result = values.addCustomProperty(def, "2022-02-24")

    val expectedDate = CalendarFactory.createGanttCalendar(2022, 1, 24)
    assertEquals(expectedDate, result?.value)
    assertEquals(expectedDate, values.getValue(def))
  }

  @Test
  fun `returns null and clears value when date string is not parseable`() {
    val manager = CustomColumnsManager()
    val def = manager.createDefinition(CustomPropertyClass.DATE, "foo")
    val values = CustomColumnsValues(manager) {}
    values.addCustomProperty(def, "2022-02-24")
    assertTrue(values.hasOwnValue(def))

    val result = values.addCustomProperty(def, "not a date")

    assertNull(result)
    assertFalse(values.hasOwnValue(def))
  }

  @Test
  fun `returns null and clears value when value string is not parseable`() {
    val manager = CustomColumnsManager()
    val def = manager.createDefinition(CustomPropertyClass.INTEGER, "foo")
    val values = CustomColumnsValues(manager) {}
    values.addCustomProperty(def, "42")
    assertTrue(values.hasOwnValue(def))

    val result = values.addCustomProperty(def, "not a number")

    assertNull(result)
    assertFalse(values.hasOwnValue(def))
  }

  @Test
  fun `returns null and clears value when value string is null`() {
    val manager = CustomColumnsManager()
    val def = manager.createDefinition(CustomPropertyClass.TEXT, "foo")
    val values = CustomColumnsValues(manager) {}
    values.addCustomProperty(def, "bar")
    assertTrue(values.hasOwnValue(def))

    val result = values.addCustomProperty(def, null)

    assertNull(result)
    assertFalse(values.hasOwnValue(def))
  }

  @Test
  fun `fires an event when a value is added`() {
    val manager = CustomColumnsManager()
    val def = manager.createDefinition(CustomPropertyClass.TEXT, "foo")
    var firedEvents = 0
    val values = CustomColumnsValues(manager) { event: CustomPropertyValueEventStub -> firedEvents++ }

    values.addCustomProperty(def, "bar")

    assertEquals(1, firedEvents)
  }

  companion object {
    @JvmStatic
    @BeforeAll
    fun beforeAll() {
      initLocale()
    }
  }
}
