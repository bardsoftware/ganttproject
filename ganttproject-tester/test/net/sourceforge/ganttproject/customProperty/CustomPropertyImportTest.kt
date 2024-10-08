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

import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyEvent
import biz.ganttproject.customproperty.CustomPropertyListener
import biz.ganttproject.customproperty.CustomColumnsManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CustomPropertyImportTest {
  @Test
  fun testImportDuplicatedProperties() {
    run {
      val target = CustomColumnsManager()
      target.createDefinition(CustomPropertyClass.TEXT, "col1", null)
      target.createDefinition(CustomPropertyClass.TEXT, "col2", null)

      val source = CustomColumnsManager()
      source.createDefinition(CustomPropertyClass.TEXT, "col1", null)
      source.createDefinition(CustomPropertyClass.TEXT, "col3", null)

      target.importData(source)
      val definitions = target.definitions
      assertEquals(3, definitions.size)
    }
    run {
      val target = CustomColumnsManager()
      target.createDefinition(CustomPropertyClass.TEXT, "col1", null)
      target.createDefinition(CustomPropertyClass.TEXT, "col2", null)

      val source = CustomColumnsManager()
      source.createDefinition(CustomPropertyClass.DATE, "col1", null)
      source.createDefinition(CustomPropertyClass.TEXT, "col3", null)

      target.importData(source)
      val definitions = target.definitions
      assertEquals(4, definitions.size)
    }
  }

  @Test
  fun `import preserves property id`() {
    val target = CustomColumnsManager()
    target.createDefinition("col1", CustomPropertyClass.TEXT.iD, "col1", null)
    target.createDefinition("col3", CustomPropertyClass.TEXT.iD, "col3", null)

    val source = CustomColumnsManager()
    source.createDefinition("col2", CustomPropertyClass.TEXT.iD, "col2", null)
    source.createDefinition("col3", CustomPropertyClass.TEXT.iD, "Column3", null)

    target.importData(source)
    val definitions = target.definitions
    assertEquals(4, definitions.size)

    assertEquals(setOf("col1", "col2", "col3", "tpc0"), definitions.map { it.id }.toSet())
  }

  @Test
  fun `import triggers only one event`() {
    val target = CustomColumnsManager()
    var eventCount = 0
    target.addListener(object: CustomPropertyListener {
      override fun customPropertyChange(event: CustomPropertyEvent) {
        eventCount++
      }
    })

    val source = CustomColumnsManager()
    source.createDefinition(CustomPropertyClass.DATE, "col1", null)
    source.createDefinition(CustomPropertyClass.TEXT, "col3", null)

    target.importData(source)
    assertEquals(1, eventCount)
  }
}
