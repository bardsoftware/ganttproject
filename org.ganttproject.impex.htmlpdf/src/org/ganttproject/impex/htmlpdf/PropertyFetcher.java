/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package org.ganttproject.impex.htmlpdf;

import java.text.DateFormat;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.Task;

class PropertyFetcher {
    private static final GanttLanguage language = GanttLanguage.getInstance();
    private final IGanttProject myProject;

    PropertyFetcher(IGanttProject project) {
        myProject = project;
    }

    String i18n(String key) {
        return language.getCorrectedLabel(key);
    }

    CustomColumnsStorage getCustomColumnStorage() {
        return myProject.getCustomColumnsStorage();
    }

    void getTaskAttributes(Task t, Map<String, String> id2value) {
        id2value.put("tpd1", i18n(t.getPriority().getI18nKey()));

        DateFormat dateFormat = language.getShortDateFormat();
        id2value.put("tpd3", t.getName());
        id2value.put("tpd4", dateFormat.format(t.getStart().getTime()));
        id2value.put("tpd5", dateFormat.format(t.getEnd().getTime()));
        id2value.put("tpd6", String.valueOf(t.getDuration().getLength()));
        id2value.put("tpd7", String.valueOf(t.getCompletionPercentage()));

        HumanResource coordinator = t.getAssignmentCollection().getCoordinator();
        if (coordinator != null) {
            id2value.put("tpd8", coordinator.getName());
        }

        CustomColumnsValues customValues = t.getCustomValues();
        for (CustomColumn column : getCustomColumnStorage().getCustomColums()) {
            Object value = customValues.getValue(column);
            String valueAsString = value == null ? "" : value.toString();
            id2value.put(column.getId(), valueAsString);
        }
    }

    void getResourceAttributes(HumanResource hr, Map<String, String> id2value) {
        id2value.put("0", hr.getName());
        id2value.put("1", hr.getRole().getName());
        id2value.put("2", hr.getMail());
        id2value.put("3", hr.getPhone());

        List<CustomProperty> customFields = hr.getCustomProperties();
        for (CustomProperty property : customFields) {
            id2value.put(property.getDefinition().getID(), property.getValueAsString());
        }
    }
}
