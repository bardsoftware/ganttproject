/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package org.ganttproject.impex.htmlpdf;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ResourceDefaultColumn;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskProperties;

public class PropertyFetcher {
  private static final GanttLanguage language = GanttLanguage.getInstance();
  private final IGanttProject myProject;

  public PropertyFetcher(IGanttProject project) {
    myProject = project;
  }

  String i18n(String key) {
    return language.getCorrectedLabel(key);
  }

  public void getTaskAttributes(Task t, Map<String, String> id2value) {
    id2value.put(TaskDefaultColumn.PRIORITY.getStub().getID(), i18n(t.getPriority().getI18nKey()));

    DateFormat dateFormat = language.getShortDateFormat();
    id2value.put(TaskDefaultColumn.NAME.getStub().getID(), t.getName());
    id2value.put(TaskDefaultColumn.BEGIN_DATE.getStub().getID(), dateFormat.format(t.getStart().getTime()));
    id2value.put(TaskDefaultColumn.END_DATE.getStub().getID(), dateFormat.format(t.getDisplayEnd().getTime()));
    id2value.put(TaskDefaultColumn.DURATION.getStub().getID(), String.valueOf(t.getDuration().getLength()));
    id2value.put(TaskDefaultColumn.COMPLETION.getStub().getID(), String.valueOf(t.getCompletionPercentage()));
    id2value.put(TaskDefaultColumn.PREDECESSORS.getStub().getID(), TaskProperties.formatPredecessors(t, ", "));
    id2value.put(TaskDefaultColumn.COORDINATOR.getStub().getID(), TaskProperties.formatCoordinators(t));
    id2value.put(TaskDefaultColumn.RESOURCES.getStub().getID(), TaskProperties.formatResources(Arrays.asList(t.getAssignments())));
    List<Integer> outlinePath = t.getManager().getTaskHierarchy().getOutlinePath(t);
    id2value.put(TaskDefaultColumn.OUTLINE_NUMBER.getStub().getID(), Joiner.on('.').join(outlinePath));
    id2value.put(TaskDefaultColumn.ID.getStub().getID(), String.valueOf(t.getTaskID()));
    id2value.put(TaskDefaultColumn.COST.getStub().getID(), t.getCost().getValue().toPlainString());

    CustomColumnsValues customValues = t.getCustomValues();
    for (CustomPropertyDefinition def : myProject.getTaskCustomColumnManager().getDefinitions()) {
      Object value = customValues.getValue(def);
      String valueAsString = value == null ? "" : value.toString();
      id2value.put(def.getID(), valueAsString);
    }
  }

  public void getResourceAttributes(HumanResource hr, Map<String, String> id2value) {
    id2value.put(ResourceDefaultColumn.NAME.getStub().getID(), hr.getName());
    id2value.put(ResourceDefaultColumn.ROLE.getStub().getID(), hr.getRole().getName());
    id2value.put(ResourceDefaultColumn.EMAIL.getStub().getID(), hr.getMail());
    id2value.put(ResourceDefaultColumn.PHONE.getStub().getID(), hr.getPhone());
    id2value.put(ResourceDefaultColumn.STANDARD_RATE.getStub().getID(), hr.getStandardPayRate().toPlainString());

    List<CustomProperty> customFields = hr.getCustomProperties();
    for (CustomProperty property : customFields) {
      id2value.put(property.getDefinition().getID(), property.getValueAsString());
    }
  }
}
