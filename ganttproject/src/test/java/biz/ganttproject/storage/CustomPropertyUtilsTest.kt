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
package biz.ganttproject.storage

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.customproperty.*
import net.sourceforge.ganttproject.storage.*
import org.jooq.Field
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [mapCustomPropertiesToJooq] which maps the values of the stored custom properties
 * into the jOOQ field-value pairs suitable for INSERT and UPDATE statements.
 */
class CustomPropertyUtilsTest {
  private val customPropertyManager = CustomColumnsManager()

  private fun emptyHolder() = CustomColumnsValues(customPropertyManager) {}

  private fun Map<Field<*>, Any?>.asNameToValue() = entries.associate { it.key.name to it.value }

  @Test
  fun `values of the stored properties come from the holder`() {
    val text = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "text")
    val int = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "int")
    val holder = emptyHolder().also {
      it.setValue(text, "hello")
      it.setValue(int, 42)
    }

    mapCustomPropertiesToJooq(customPropertyManager, holder).asNameToValue().also {
      assertEquals("hello", it[text.id])
      assertEquals(42, it[int.id])
    }
  }

  @Test
  fun `fields are named after the property ids`() {
    val text = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "text")
    val int = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "int")

    mapCustomPropertiesToJooq(customPropertyManager, emptyHolder()).asNameToValue().also {
      assertTrue(it.containsKey(text.id))
      assertTrue(it.containsKey(int.id))
    }
  }

  @Test
  fun `missing values fall back to the default values`() {
    val text = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "text", "default text")
    val int = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "int", "10")

    mapCustomPropertiesToJooq(customPropertyManager, emptyHolder()).asNameToValue().also {
      assertEquals("default text", it[text.id])
      assertEquals(10, it[int.id])
    }
  }

  @Test
  fun `properties with neither own nor default value map to null`() {
    val text = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "text")

    mapCustomPropertiesToJooq(customPropertyManager, emptyHolder()).asNameToValue().also {
      assertTrue(it.containsKey(text.id))
      assertNull(it[text.id])
    }
  }

  @Test
  fun `calculated properties are excluded`() {
    val stored = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "stored")
    val calculated = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "calculated").also {
      it.calculationMethod = SimpleSelect(it.id, "${stored.id} * 2", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }

    mapCustomPropertiesToJooq(customPropertyManager, emptyHolder()).asNameToValue().also {
      assertEquals(setOf(stored.id), it.keys)
      assertFalse(it.containsKey(calculated.id))
    }
  }

  @Test
  fun `calendar values of the date properties are converted to LocalDate`() {
    val date = customPropertyManager.createDefinition(CustomPropertyClass.DATE, "date")
    val holder = emptyHolder().also {
      it.setValue(date, CalendarFactory.createGanttCalendar(2024, 4, 1))
    }

    mapCustomPropertiesToJooq(customPropertyManager, holder).asNameToValue().also {
      assertEquals(LocalDate.of(2024, 5, 1), it[date.id])
    }
  }

  @Test
  fun `numbers are converted to Double for the double properties`() {
    val double = customPropertyManager.createDefinition(CustomPropertyClass.DOUBLE, "double")
    // A holder may keep a value of a type which is different from the property type,
    // e.g. when it comes from a foreign data source.
    val holder = stubHolder(mapOf(double to 2))

    mapCustomPropertiesToJooq(customPropertyManager, holder).asNameToValue().also {
      assertEquals(2.0, it[double.id])
    }
  }
}

private fun stubHolder(values: Map<CustomPropertyDefinition, Any?>): CustomPropertyHolder {
  val props = values.map { (def, value) ->
    object : CustomProperty {
      override val definition = def
      override val value = value
      override val valueAsString = value?.toString() ?: ""
    }
  }
  return object : CustomPropertyHolder {
    override fun getCustomProperties() = props
    override fun addCustomProperty(definition: CustomPropertyDefinition, defaultValueAsString: String?): CustomProperty? = null
    override fun setValue(def: CustomPropertyDefinition, value: Any?) {}
    override fun getValue(def: CustomPropertyDefinition): Any? = null
  }
}
