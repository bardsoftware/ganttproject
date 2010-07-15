package net.sourceforge.ganttproject.io;

import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.xml.transform.sax.TransformerHandler;

import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.util.ColorConvertion;

import org.w3c.util.DateParser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

class TaskSaver extends SaverBase {
    void save(IGanttProject project, TransformerHandler handler, Color defaultColor) throws SAXException, IOException {
        AttributesImpl attrs = new AttributesImpl();
        if (defaultColor!=null) {
        	addAttribute("color", ColorConvertion.getColor(defaultColor), attrs);
        }
        startElement("tasks", attrs, handler);

        startElement("taskproperties", handler);
        writeTaskProperties(handler, project.getCustomColumnsStorage());
        endElement("taskproperties", handler);
        Task rootTask = project.getTaskManager().getTaskHierarchy().getRootTask();
        Task[] tasks = project.getTaskManager().getTaskHierarchy().getNestedTasks(rootTask);
        for (int i=0; i<tasks.length; i++) {
            writeTask(handler, (GanttTask) tasks[i], project.getCustomColumnsStorage());
        }
        endElement("tasks", handler);
    }

    private void writeTask(TransformerHandler handler, GanttTask task, CustomColumnsStorage customColumns) throws SAXException, IOException {
        if (task.getTaskID() == -1) {
            throw new IllegalArgumentException("Is it a fake root task? Task="+task);
        }
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("id", String.valueOf(task.getTaskID()), attrs);
        addAttribute("name", task.getName(), attrs);
        if (task.colorDefined()) {
            addAttribute("color", ColorConvertion.getColor(task.getColor()), attrs);
        }
        if (task.shapeDefined()) {
            addAttribute("shape", task.getShape().getArray(), attrs);
        }
        addAttribute("meeting", Boolean.valueOf(task.isMilestone()).toString(), attrs);
        if (task.isProjectTask()) {
            addAttribute("project", Boolean.TRUE.toString(), attrs);
        }
        addAttribute("start", task.getStart().toXMLString(), attrs);
        addAttribute("duration", String.valueOf(task.getLength()), attrs);
        addAttribute("complete", String.valueOf(task.getCompletionPercentage()), attrs);
        if (task.getThird() != null) {
            addAttribute("thirdDate", task.getThird().toXMLString(), attrs);
            addAttribute("thirdDate-constraint", String.valueOf(task.getThirdDateConstraint()), attrs);
        }
        addAttribute("priority", String.valueOf(task.getPriority()), attrs);
        final String sWebLink = task.getWebLink();
        if (sWebLink != null && !sWebLink.equals("")
                && !sWebLink.equals("http://")) {
            addAttribute("webLink", URLEncoder.encode(sWebLink, "ISO-8859-1"), attrs);
        }
        addAttribute("expand", String.valueOf(task.getExpand()), attrs);

        startElement("task", attrs, handler);

        if (task.getNotes() != null && task.getNotes().length() > 0) {
            cdataElement("notes", task.getNotes(), attrs, handler);
//            fout.write(space2 + "<notes>");
//            fout.write("\n"
//                    + space2
//                    + s
//                    + correct(replaceAll(task.getNotes(), "\n", "\n"
//                            + space2 + s)));
//            fout.write("\n" + space2 + "</notes>\n");
        }
        // use successors to write depends information
        final TaskDependency[] depsAsDependee = task.getDependenciesAsDependee().toArray();
        for (int i = 0; i < depsAsDependee.length; i++) {
            TaskDependency next = depsAsDependee[i];
            addAttribute("id", String.valueOf(next.getDependant().getTaskID()), attrs);
            addAttribute("type", String.valueOf(next.getConstraint().getID()), attrs);
            addAttribute("difference", String.valueOf(next.getDifference()), attrs);
            addAttribute("hardness", next.getHardness().getIdentifier(), attrs);
            emptyElement("depend", attrs, handler);
        }

        CustomColumnsValues ccv = task.getCustomValues();
        for (Iterator/*<CustomColumn>*/ it = customColumns.getCustomColums().iterator(); it.hasNext();) {
            CustomColumn nextColumn = (CustomColumn) it.next();
            final String name = nextColumn.getName();
            final String idc = nextColumn.getId();
            Object value = ccv.getValue(name);
            if (GregorianCalendar.class.isAssignableFrom(nextColumn.getType()) && value!=null) {
                value = DateParser.getIsoDate(((GanttCalendar)value).getTime());
            }
            addAttribute("taskproperty-id", idc, attrs);
            addAttribute("value", value==null ? null : String.valueOf(value), attrs);
            emptyElement("customproperty", attrs, handler);
        }

        // Write the child of the task
        if (task.getManager().getTaskHierarchy().hasNestedTasks(task)) {
            Task[] nestedTasks = task.getManager().getTaskHierarchy().getNestedTasks(task);
            for (int i = 0; i < nestedTasks.length; i++) {
                writeTask(handler, (GanttTask) nestedTasks[i], customColumns);
            }

        }

        // end of task section
        endElement("task", handler);
    }

    private void writeTaskProperty(TransformerHandler handler, String id, String name, String type, String valueType) throws SAXException {
        writeTaskProperty(handler, id, name, type, valueType, null);
    }
    private void writeTaskProperty(TransformerHandler handler, String id, String name, String type, String valueType, String defaultValue) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("id", id, attrs);
        addAttribute("name", name, attrs);
        addAttribute("type", type, attrs);
        addAttribute("valuetype", valueType, attrs);
        if (defaultValue!=null) {
            addAttribute("defaultvalue", defaultValue, attrs);
        }
        emptyElement("taskproperty", attrs, handler);
    }
    private void writeTaskProperties(TransformerHandler handler, CustomColumnsStorage customCol) throws SAXException {
        writeTaskProperty(handler, "tpd0", "type", "default", "icon");
        writeTaskProperty(handler, "tpd1", "priority", "default", "icon");
        writeTaskProperty(handler, "tpd2", "info", "default", "icon");
        writeTaskProperty(handler, "tpd3", "name", "default", "text");
        writeTaskProperty(handler, "tpd4", "begindate", "default", "date");
        writeTaskProperty(handler, "tpd5", "enddate", "default", "date");
        writeTaskProperty(handler, "tpd6", "duration", "default", "int");
        writeTaskProperty(handler, "tpd7", "completion", "default", "int");
        writeTaskProperty(handler, "tpd8", "coordinator", "default", "text");
        writeTaskProperty(handler, "tpd9", "predecessorsr", "default", "text");
        Iterator/*<CustomColumn>*/ it = customCol.getCustomColums().iterator();
        while (it.hasNext()) {
            final CustomColumn cc = (CustomColumn) it.next();
            Object defVal = cc.getDefaultValue();
            final Class cla = cc.getType();
            final String valueType = encodeFieldType(cla);
            if (valueType==null) {
            	continue;
            }
            if ("date".equals(valueType) && defVal!=null){
            	assert defVal instanceof GanttCalendar;
            	defVal = DateParser.getIsoDate(((GanttCalendar)defVal).getTime());
            }
            String idcStr = cc.getId();
            writeTaskProperty(handler, idcStr, cc.getName(), "custom", valueType, defVal==null ? null : String.valueOf(defVal));
        }
    }

    static String encodeFieldType(Class fieldType) {
    	return CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(fieldType);
    }

}
