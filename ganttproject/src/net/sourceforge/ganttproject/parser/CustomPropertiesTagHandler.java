/*
 * Created on Mar 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.parser;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;
import org.xml.sax.Attributes;

/**
 * @author bbaranne Mar 10, 2005
 */
public class CustomPropertiesTagHandler implements TagHandler, ParsingListener {
    private TaskManager taskManager = null;

    private ParsingContext parsingContext = null;

    private List<CustomPropertiesStructure> listStructure = null;

	private final CustomColumnsStorage myColumnStorage;

    public CustomPropertiesTagHandler(ParsingContext context,
            TaskManager taskManager, CustomColumnsStorage columnStorage) {
        this.taskManager = taskManager;
        this.parsingContext = context;
        this.listStructure = new ArrayList<CustomPropertiesStructure>();
        myColumnStorage = columnStorage;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) throws FileFormatException {
        if (qName.equals("customproperty"))
            loadProperty(attrs);

    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void endElement(String namespaceURI, String sName, String qName) {
        // nothing to do.
    }

    /**
     * @see net.sourceforge.ganttproject.parser.ParsingListener#parsingStarted()
     */
    public void parsingStarted() {
        // nothing to do.
    }

    /**
     * @see net.sourceforge.ganttproject.parser.ParsingListener#parsingFinished()
     */
    public void parsingFinished() {
        Iterator<CustomPropertiesStructure> it = this.listStructure.iterator();

        while (it.hasNext()) {
            CustomPropertiesStructure cps = it
                    .next();
            Task task = taskManager.getTask(cps.taskID);
            CustomColumn cc = myColumnStorage.getCustomColumnByID(cps.taskPropertyID);
            String valueStr = cps.value;
            Object value = null;
            Class cla = cc.getType();

            if (valueStr!=null) {
	            if (cla.equals(String.class))
	                value = valueStr.toString();
	            else if (cla.equals(Boolean.class))
	                value = Boolean.valueOf(valueStr);
	            else if (cla.equals(Integer.class))
	                value = Integer.valueOf(valueStr);
	            else if (cla.equals(Double.class))
	                value = Double.valueOf(valueStr);
	            else if (GregorianCalendar.class.isAssignableFrom(cla))
	                try {
	                    value = new GanttCalendar(DateParser.parse(valueStr));
	                } catch (InvalidDateException e) {
	                	if (!GPLogger.log(e)) {
	                		e.printStackTrace(System.err);
	                	}
	                }
            }
            try {
                // System.out.println(task.getName());
                task.getCustomValues().setValue(cc.getName(), value);
            } catch (CustomColumnsException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            }
        }
    }

    private void loadProperty(Attributes attrs) {
        if (attrs != null) {
            // System.out.println(parsingContext.getTaskID());
            CustomPropertiesStructure cps = new CustomPropertiesStructure();
            cps.setTaskID(this.parsingContext.getTaskID());
            cps.setTaskPropertyID(attrs.getValue("taskproperty-id"));
            cps.setValue(attrs.getValue("value"));

            this.listStructure.add(cps);
        }
    }

    private class CustomPropertiesStructure {
        public int taskID;

        public String taskPropertyID = null;

        public String value = null;

        public CustomPropertiesStructure() {
        }

        public void setTaskID(int taskID) {
            this.taskID = taskID;
        }

        public void setTaskPropertyID(String propertyID) {
            this.taskPropertyID = propertyID;
        }

        public void setValue(String val) {
            this.value = val;
        }
    }
}
