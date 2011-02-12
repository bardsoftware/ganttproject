package org.ganttproject.impex.htmlpdf;

import java.text.DateFormat;
import java.util.Iterator;
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
    private final IGanttProject myProject;
    PropertyFetcher(IGanttProject project) {
        myProject = project;
    }
    String i18n(String key) {
        String text = GanttLanguage.getInstance().getText(key);
        return GanttLanguage.getInstance().correctLabel(text);
    }
    CustomColumnsStorage getCustomColumnStorage() {
        return myProject.getCustomColumnsStorage();
    }    
    void getTaskAttributes(Task t, Map<String, String> id2value) {
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
        for (Iterator<CustomColumn> it = getCustomColumnStorage()
                .getCustomColums().iterator(); it.hasNext();) {
            CustomColumn nextColumn = it.next();
            Object value = customValues.getValue(nextColumn.getName());
            String valueAsString = value==null ? "" : value.toString();
            id2value.put(nextColumn.getId(), valueAsString);
        }
    }

    void getResourceAttributes(HumanResource hr, Map<String, String> id2value) {
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
