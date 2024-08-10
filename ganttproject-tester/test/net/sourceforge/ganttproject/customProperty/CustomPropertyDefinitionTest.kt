/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
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
import biz.ganttproject.customproperty.*
import net.sourceforge.ganttproject.task.CustomColumnsManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.text.DateFormat
import java.util.*

fun initLocale() {
  object : CalendarFactory() {
    init {
      setLocaleApi(object : LocaleApi {
        override fun getLocale(): Locale {
          return Locale.US
        }

        override fun getShortDateFormat(): DateFormat {
          return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        }
      })
    }
  }
}

class CustomPropertyDefinitionTest {
  @Test
  fun `identifiers are incremented`() {
    val customPropertyManager = CustomColumnsManager()
    val def1 = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "foo")
    assertEquals("tpc0", def1.id)
    val def2 = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "bar")
    assertEquals("tpc1", def2.id)
  }

  @Test
  fun `existing property identifier`() {
    val customPropertyManager = CustomColumnsManager()
    val def1 = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "foo")
    assertThrows<CustomColumnsException> {
      customPropertyManager.createDefinition("tpc0", CustomPropertyClass.TEXT.iD, "foo")
    }
  }

  @Test
  fun `default values`() {
    val customPropertyManager = CustomColumnsManager()
    customPropertyManager.createDefinition(CustomPropertyClass.BOOLEAN, "foo", defValue = "true").let {
      assertEquals(true, it.defaultValue)
    }
    customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "foo", defValue = "10").let {
      assertEquals(10, it.defaultValue)
    }
    customPropertyManager.createDefinition(CustomPropertyClass.DOUBLE, "foo", defValue = "3.14").let {
      assertEquals(3.14, it.defaultValue)
    }
    customPropertyManager.createDefinition(CustomPropertyClass.DATE, "foo", defValue = "2022-02-24").let {
      assertEquals(CalendarFactory.createGanttCalendar(2022, 1, 24), it.defaultValue)
    }
  }

  @Test
  fun `events firing`() {
    val customPropertyManager = CustomColumnsManager()
    val def1 = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "foo")

    fun assertEventFired(code: ()->Unit) {
      var eventFired = false
      val listener = object: CustomPropertyListener {
        override fun customPropertyChange(event: CustomPropertyEvent) {
          eventFired = true
        }
      }
      customPropertyManager.addListener(listener)
      code()
      assertTrue(eventFired)
      customPropertyManager.removeListener(listener)
    }

    assertEventFired {
      def1.name = "bar"
    }
    assertEventFired {
      def1.propertyClass = CustomPropertyClass.INTEGER
    }
    assertEventFired {
      def1.calculationMethod = SimpleSelect(def1.id, "1", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }

    assertEventFired {
      customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "bar")
    }
    customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "baz").let {
      assertEventFired {
        customPropertyManager.deleteDefinition(it)
      }
    }




  }


  companion object {
    @JvmStatic
    @BeforeAll
    fun beforeAll(): Unit {
      initLocale()
    }
  }

}