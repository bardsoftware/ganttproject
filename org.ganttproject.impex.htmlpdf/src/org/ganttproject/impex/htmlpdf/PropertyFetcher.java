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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.Task;

public class PropertyFetcher {
    private final IGanttProject myProject;
    public PropertyFetcher(IGanttProject project) {
        myProject = project;
    }
    String i18n(String key) {
        String text = GanttLanguage.getInstance().getText(key);
        return GanttLanguage.getInstance().correctLabel(text);
    }

    public void getTaskAttributes(Task t, Map<String, String> id2value) {
        id2value.put("tpd1", i18n(t.getPriority().getI18nKey()));

        DateFormat dateFormat = GanttLanguage.getInstance().getShortDateFormat();
        id2value.put("tpd3", t.getName());
        id2value.put("tpd4", dateFormat.format(t.getStart().getTime()));
        id2value.put("tpd5", dateFormat.format(t.getEnd().getTime()));
        id2value.put("tpd6", String.valueOf(t.getDuration().getLength()));
        id2value.put("tpd7", String.valueOf(t.getCompletionPercentage()));

        HumanResource coordinator = t.getAssignmentCollection().getCoordinator();
        if (coordinator!=null) {
            id2value.put("tpd8", coordinator.getName());
        }

        CustomColumnsValues customValues = t.getCustomValues();
        for (CustomPropertyDefinition def : myProject.getTaskCustomColumnManager().getDefinitions()) {
            Object value = customValues.getValue(def);
            String valueAsString = value==null ? "" : value.toString();
            id2value.put(def.getID(), valueAsString);
        }
    }

    public void getResourceAttributes(HumanResource hr, Map<String, String> id2value) {
        id2value.put("0", hr.getName());
        id2value.put("1", hr.getRole().getName());
        id2value.put("2", hr.getMail());
        id2value.put("3", hr.getPhone());

        List<CustomProperty> customFields = hr.getCustomProperties();
        for (int j=0; j<customFields.size(); j++) {
            CustomProperty nextProperty = (CustomProperty) customFields.get(j);
            id2value.put(nextProperty.getDefinition().getID(),nextProperty.getValueAsString());
        }
    }
}
