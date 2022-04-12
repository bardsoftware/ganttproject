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

import biz.ganttproject.core.io.XmlProject
import net.sourceforge.ganttproject.roles.RoleManager
import net.sourceforge.ganttproject.roles.RolePersistentID

class RoleSerializer(private val roleManager: RoleManager) {
  fun loadRoles(xmlProject: XmlProject) {
    xmlProject.roles.forEach {
      val roleSet = if (it.rolesetName == null) {
        roleManager.projectRoleSet
      } else {
        roleManager.getRoleSet(it.rolesetName) ?: roleManager.createRoleSet(it.rolesetName)
      }
      it.roles?.forEach { xmlRole ->
        val roleName = xmlRole.name
        val persistentID = RolePersistentID(xmlRole.id)
        roleSet.findRole(persistentID.roleID) ?: roleSet.createRole(roleName, persistentID.roleID)
      }
    }
  }
}