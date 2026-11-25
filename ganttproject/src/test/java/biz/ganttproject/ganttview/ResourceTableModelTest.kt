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
package biz.ganttproject.ganttview

import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.customproperty.CustomColumnsManager
import net.sourceforge.ganttproject.ResourceDefaultColumn
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.RoleManagerImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import net.sourceforge.ganttproject.resource.ResourceEvent
import net.sourceforge.ganttproject.resource.ResourceView
import java.math.BigDecimal

/**
 * Tests for the resource table model. See issue #2899: the standard rate entered in the table was ignored.
 */
class ResourceTableModelTest {
  private val roleManager = RoleManagerImpl()
  private val resourceManager = HumanResourceManager(roleManager.defaultRole, CustomColumnsManager(), roleManager)
  private val model = ResourceTableModel { false }

  private fun newResource() = HumanResource("Joe", 1, resourceManager).also { resourceManager.add(it) }

  @Test
  fun `standard rate is set from the decimal value that the table cell provides`() {
    val resource = newResource()
    model.setValue(BigDecimal("42.5"), ResourceNode(resource), ResourceDefaultColumn.STANDARD_RATE)
    assertEquals(BigDecimal("42.5"), resource.standardPayRate)
  }

  @Test
  fun `standard rate accepts other numeric values`() {
    val resource = newResource()
    model.setValue(10.0, ResourceNode(resource), ResourceDefaultColumn.STANDARD_RATE)
    assertEquals(0, BigDecimal.TEN.compareTo(resource.standardPayRate))
  }

  @Test
  fun `hours per day are set from the decimal value of the cell and a blank cell clears them`() {
    val resource = newResource()
    model.setValue(BigDecimal("6.5"), ResourceNode(resource), ResourceDefaultColumn.HOURS_PER_DAY)
    assertEquals(BigDecimal("6.5"), resource.hoursPerDay)
    model.setValue(null, ResourceNode(resource), ResourceDefaultColumn.HOURS_PER_DAY)
    assertNull(resource.hoursPerDay)
  }

  @Test
  fun `hours per day outside of 0 to 24 are rejected`() {
    val resource = newResource()
    model.setValue(BigDecimal("8"), ResourceNode(resource), ResourceDefaultColumn.HOURS_PER_DAY)
    assertThrows(ValidationException::class.java) {
      model.setValue(BigDecimal("-1"), ResourceNode(resource), ResourceDefaultColumn.HOURS_PER_DAY)
    }
    assertThrows(ValidationException::class.java) {
      model.setValue(BigDecimal("24.5"), ResourceNode(resource), ResourceDefaultColumn.HOURS_PER_DAY)
    }
    assertEquals(BigDecimal("8"), resource.hoursPerDay)
    model.setValue(BigDecimal("24"), ResourceNode(resource), ResourceDefaultColumn.HOURS_PER_DAY)
    model.setValue(BigDecimal.ZERO, ResourceNode(resource), ResourceDefaultColumn.HOURS_PER_DAY)
    assertEquals(BigDecimal.ZERO, resource.hoursPerDay)
  }

  @Test
  fun `standard rate change notifies resource listeners`() {
    val resource = newResource()
    var changed = 0
    resourceManager.addView(object : ResourceView {
      override fun resourceAdded(event: ResourceEvent) {}
      override fun resourcesRemoved(event: ResourceEvent) {}
      override fun resourceChanged(e: ResourceEvent) { changed++ }
      override fun resourceAssignmentsChanged(e: ResourceEvent) {}
      override fun resourceStructureChanged() {}
      override fun resourceModelReset() {}
    })
    model.setValue(BigDecimal.ONE, ResourceNode(resource), ResourceDefaultColumn.STANDARD_RATE)
    assertEquals(1, changed)
  }

  @Test
  fun `hours per day change notifies resource listeners`() {
    val resource = newResource()
    var changed = 0
    resourceManager.addView(object : ResourceView {
      override fun resourceAdded(event: ResourceEvent) {}
      override fun resourcesRemoved(event: ResourceEvent) {}
      override fun resourceChanged(e: ResourceEvent) { changed++ }
      override fun resourceAssignmentsChanged(e: ResourceEvent) {}
      override fun resourceStructureChanged() {}
      override fun resourceModelReset() {}
    })
    resource.hoursPerDay = BigDecimal("6")
    assertEquals(1, changed)
  }
}
