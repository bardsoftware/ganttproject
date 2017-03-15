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
package biz.ganttproject.impex.csv;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.sourceforge.ganttproject.ResourceDefaultColumn;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import org.apache.commons.csv.CSVRecord;

import java.util.List;

/**
 * Class responsible for processing resource records in CSV import
 *
 * @author dbarashev (Dmitry Barashev)
 */
class ResourceRecords extends RecordGroup {

  public enum ResourceFields {
    ID("tableColID"), NAME("tableColResourceName"), EMAIL("tableColResourceEMail"), PHONE("tableColResourcePhone"), ROLE("tableColResourceRole");

    private final String text;

    private ResourceFields(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      // Return translated field name
      return GanttLanguage.getInstance().getText(text);
    }
  }

  private final HumanResourceManager resourceManager;
  private final RoleManager myRoleManager;

  ResourceRecords(HumanResourceManager resourceManager, RoleManager roleManager) {
    super("Resource group",
      Sets.union(
          Sets.newHashSet(GanttCSVOpen.getFieldNames(ResourceFields.values())),
          ImmutableSet.of(ResourceDefaultColumn.STANDARD_RATE.getName())),
      Sets.newHashSet(GanttCSVOpen.getFieldNames(ResourceFields.ID, ResourceFields.NAME)));
    this.resourceManager = Preconditions.checkNotNull(resourceManager);
    myRoleManager = Preconditions.checkNotNull(roleManager);
  }
  @Override
  public void setHeader(List<String> header) {
    super.setHeader(header);
    GanttCSVOpen.createCustomProperties(getCustomFields(), resourceManager.getCustomPropertyManager());
  }

  @Override
  protected boolean doProcess(CSVRecord record) {
    if (!super.doProcess(record)) {
      return false;
    }
    if (!hasMandatoryFields(record)) {
      return false;
    }
    assert record.size() > 0;
    String role = getOrNull(record, ResourceFields.ROLE.toString());
    if (role != null && myRoleManager.getRole(role) == null) {
      Role newRole = myRoleManager.getProjectRoleSet().createRole(role);
      role = newRole.getPersistentID();
    }
    HumanResource hr = resourceManager.newResourceBuilder()
        .withName(getOrNull(record, ResourceFields.NAME.toString()))
        .withID(getOrNull(record, ResourceFields.ID.toString()))
        .withEmail(getOrNull(record, ResourceFields.EMAIL.toString()))
        .withPhone(getOrNull(record, ResourceFields.PHONE.toString()))
        .withRole(role)
        .withStandardRate(getOrNull(record, ResourceDefaultColumn.STANDARD_RATE.getName()))
        .build();
    for (String customField : getCustomFields()) {
      String value = getOrNull(record, customField);
      if (value != null) {
        hr.addCustomProperty(resourceManager.getCustomPropertyManager().getCustomPropertyDefinition(customField), value);
      }
    }
    return true;
  }
}