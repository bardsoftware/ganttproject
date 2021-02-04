/*
Copyright 2014 BarD Software s.r.o

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
package biz.ganttproject.impex.csv

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import net.sourceforge.ganttproject.ResourceDefaultColumn
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.RoleManager

/**
 * Class responsible for processing resource records in CSV import
 *
 * @author dbarashev (Dmitry Barashev)
 */
internal class ResourceRecords(
    private val resourceManager: HumanResourceManager,
    private val myRoleManager: RoleManager) : RecordGroup("Resource group",
      Sets.union(
          Sets.newHashSet(GanttCSVOpen.getFieldNames(*ResourceFields.values())),
          ImmutableSet.of(ResourceDefaultColumn.STANDARD_RATE.getName())
      ),
      Sets.newHashSet(GanttCSVOpen.getFieldNames(ResourceFields.ID, ResourceFields.NAME))
    ) {

  enum class ResourceFields(private val text: String) {
    ID("tableColID"), NAME("tableColResourceName"), EMAIL("tableColResourceEMail"), PHONE("tableColResourcePhone"), ROLE("tableColResourceRole");

    override fun toString(): String {
      // Return translated field name
      return GanttLanguage.getInstance().getText(text)
    }
  }

  override fun setHeader(header: List<String>) {
    super.setHeader(header)
    //    GanttCSVOpen.createCustomProperties(getCustomFields(), resourceManager.getCustomPropertyManager());
  }

  override fun doProcess(record: SpreadsheetRecord): Boolean {
    if (!super.doProcess(record)) {
      return false
    }
    if (!hasMandatoryFields(record)) {
      return false
    }
    assert(record.size() > 0)
    var role = getOrNull(record, ResourceFields.ROLE.toString())
    if (role != null && myRoleManager.getRole(role) == null) {
      val newRole = myRoleManager.projectRoleSet.createRole(role)
      role = newRole.persistentID
    }
    val hr = resourceManager.newResourceBuilder()
        .withName(getOrNull(record, ResourceFields.NAME.toString()))
        .withID(record.getInt(ResourceFields.ID.toString()))
        .withEmail(getOrNull(record, ResourceFields.EMAIL.toString()))
        .withPhone(getOrNull(record, ResourceFields.PHONE.toString()))
        .withRole(role)
        .withStandardRate(record.getBigDecimal(ResourceDefaultColumn.STANDARD_RATE.getName()))
        .build()
    readCustomProperties(names = customFields, record = record, customPropertyMgr = resourceManager.customPropertyManager) { def, value -> hr.addCustomProperty(def, value)
    }
    return true
  }

}
