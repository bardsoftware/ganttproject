/**
 * 
 */
package net.sourceforge.ganttproject.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttPreviousStateTask;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author nbohn
 * 
 */
public class PreviousStateTasksTagHandler extends DefaultHandler implements
        TagHandler {
    private String myName = "";

    private GanttPreviousState previousState;

    private final List<GanttPreviousState> myPreviousStates;

    private ArrayList<GanttPreviousStateTask> tasks = new ArrayList<GanttPreviousStateTask>();

    public PreviousStateTasksTagHandler(List<GanttPreviousState> previousStates) {
        myPreviousStates = previousStates;
    }

    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {
        if (qName.equals("previous-tasks")) {
            setName(attrs.getValue("name"));
            tasks = new ArrayList<GanttPreviousStateTask>();
            if (myPreviousStates != null) {
                try {
                    previousState = new GanttPreviousState(myName);
                    myPreviousStates.add(previousState);
                } catch (IOException e) {
                	if (!GPLogger.log(e)) {
                		e.printStackTrace(System.err);
                	}
                }
            }
        } else if ((qName.equals("previous-task"))) {// && (myPreviousStates
            // != null)) {
            writePreviousTask(attrs);
        }
    }

    public void endElement(String namespaceURI, String sName, String qName) {
        if ((qName.equals("previous-tasks")) && (myPreviousStates != null)) {
            previousState.saveFilesFromLoaded(tasks);
        }
    }

    private void setName(String name) {
        myName = name;
    }

    private void writePreviousTask(Attributes attrs) {

        String id = attrs.getValue("id");

        boolean meeting = Boolean.parseBoolean(attrs.getValue("meeting"));

        String start = attrs.getValue("start");

        String duration = attrs.getValue("duration");

        boolean nested = Boolean.parseBoolean(attrs.getValue("super"));

        GanttPreviousStateTask task = new GanttPreviousStateTask(
                new Integer(id).intValue(), GanttCalendar.parseXMLDate(start),
                new Integer(duration).intValue(), meeting, nested);
        tasks.add(task);
    }

    public String getName() {
        return myName;
    }

    public ArrayList<GanttPreviousStateTask> getTasks() {
        return tasks;
    }
}
