/*
Copyright 2022 BarD Software s.r.o, GanttProject Cloud OU, Dmitry Barashev

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.parser

import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.io.XmlProject
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.customproperty.CustomPropertyManager
import net.sourceforge.ganttproject.gui.zoom.ZoomManager
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.Role
import net.sourceforge.ganttproject.roles.RoleManager
import net.sourceforge.ganttproject.roles.RolePersistentID
import net.sourceforge.ganttproject.roles.RoleSet

class ResourceLoader(private val resourceManager: HumanResourceManager, private val roleManager: RoleManager,
                     private val customPropertyManager: CustomPropertyManager) {
  fun loadResources(xmlProject: XmlProject) {
    loadCustomPropertyDefinitions(xmlProject)
    xmlProject.resources.resources.forEach { xmlResource ->
      val resource = resourceManager.create(xmlResource.name, xmlResource.id).also { hr ->
        hr.mail = xmlResource.email
        hr.phone = xmlResource.phone
        val roleID = xmlResource.role
        if (roleID.isNotBlank()) {
          roleManager.findRole(roleID)?.let { hr.role = it }
        }
      }
      xmlResource.rate?.let { xmlRate ->
        if (xmlRate.name == "standard") {
          resource.standardPayRate = xmlRate.value
        }
      }
      xmlResource.props.forEach { xmlCustomProperty ->
        customPropertyManager.getCustomPropertyDefinition(xmlCustomProperty.definitionId)?.let { def ->
          resource.addCustomProperty(def, xmlCustomProperty.value)
        } ?: run {
          // LOG
        }
      }
    }
    loadVacations(xmlProject)
  }

  private fun loadCustomPropertyDefinitions(xmlProject: XmlProject) {
    xmlProject.resources.customProperties.forEach {
      customPropertyManager.createDefinition(it.id, it.type, it.name, it.defaultValue)
    }
  }

  private fun loadVacations(xmlProject: XmlProject) {
    xmlProject.vacations.forEach { xmlVacation ->
      // <vacation start="2005-04-14" end="2005-04-14" resourceid="0"/>
      resourceManager.getById(xmlVacation.resourceid)?.let {
        val startDate = xmlVacation.startDate
        val endDate = xmlVacation.endDate
        if (startDate.isNotBlank() && endDate.isNotBlank()) {
          it.addDaysOff(GanttDaysOff(GanttCalendar.parseXMLDate(startDate), GanttCalendar.parseXMLDate(endDate)))
        }
      } ?: run {
        // LOG
      }
    }
  }
}

fun RoleManager.findRole(id: String): Role? {
  val persistentID = RolePersistentID(id)
  val rolesetName = persistentID.roleSetID
  val roleID = persistentID.roleID
  var roleSet: RoleSet
  if (rolesetName == null) {
    roleSet = this.projectRoleSet
    if (roleSet.findRole(roleID) == null) {
      if (roleID in 3..10) {
        roleSet = this.getRoleSet(RoleSet.SOFTWARE_DEVELOPMENT)
        roleSet.isEnabled = true
      } else if (roleID <= 2) {
        roleSet = this.getRoleSet(RoleSet.DEFAULT)
      }
    }
  } else {
    roleSet = this.getRoleSet(rolesetName)
  }
  return roleSet.findRole(roleID)
}

fun loadResourceView(xmlProject: XmlProject, zoomManager: ZoomManager, resourceColumns: ColumnList) {
  val xmlView = xmlProject.views.filter { "resource-table" == it.id }.firstOrNull() ?: return
  loadView(xmlView, zoomManager, resourceColumns)
}
