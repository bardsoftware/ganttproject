package net.sourceforge.ganttproject.parser;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Stack;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.shape.ShapePaint;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;

import org.xml.sax.Attributes;

public class TaskTagHandler implements TagHandler {
    public TaskTagHandler(TaskManager mgr, ParsingContext context) {
        myManager = mgr;
        myContext = context;
    }

    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {
        if (qName.equals("task")) {
            loadTask(attrs);
        }
    }

    /** Method when finish to parse an attibute */
    public void endElement(String namespaceURI, String sName, String qName) {
        if (qName.equals("task")) {
            myStack.pop();
        }
    }

    private void loadTask(Attributes attrs) {
        String taskIdAsString = attrs.getValue("id");
        GanttTask task = null;
        if (taskIdAsString == null) {
            task = getManager().createTask();
        } else {
            int taskId;
            try {
                taskId = Integer.parseInt(taskIdAsString);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Failed to parse the value '"
                        + taskIdAsString + "' of attribute 'id' of tag <task>",
                        e);
            }
            task = getManager().createTask(taskId);
        }
        //
        String taskName = attrs.getValue("name");
        if (taskName != null) {
            task.setName(taskName);
        }

        task.setMilestone(Boolean.parseBoolean(attrs.getValue("meeting")));

        String project = attrs.getValue("project");
        if (project != null)
            task.setProjectTask(true);

        String start = attrs.getValue("start");
        if (start != null) {
            task.setStart(GanttCalendar.parseXMLDate(start));
        }

        String duration = attrs.getValue("duration");
        if (duration != null) {
            try {
                int length = Integer.parseInt(duration);
                if (length == 0) {
                    length = 1;
                }
                task.setLength(length);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Failed to parse the value '"
                        + duration + "' of attribute 'duration' of tag <task>",
                        e);
            }
        }

        String complete = attrs.getValue("complete");
        if (complete != null) {
            try {
                task.setCompletionPercentage(Integer.parseInt(complete));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Failed to parse the value '"
                        + complete + "' of attribute 'complete' of tag <task>",
                        e);
            }
        }

        String priority = attrs.getValue("priority");
        if (priority != null) {
            try {
                int old_p = Integer.parseInt(priority);
                // old_p contains old priority values, so convert them
                Task.Priority p;
                switch(old_p) {
                    case 0:
                        p = Task.Priority.LOW;
                        break;
                    case 2:
                        p = Task.Priority.HIGH;
                        break;
                    default:
                        p = Task.Priority.NORMAL;
                }
                task.setPriority(p);
            } catch (NumberFormatException nfe) {
                try {
                    task.setPriority(Task.Priority.valueOf(priority.toUpperCase()));
                } catch(IllegalArgumentException e) {
                    throw new RuntimeException("Failed to parse the value '"
                        + priority + "' of attribute 'priority' of tag <task>",
                        e);
                }
            }
        }

        String color = attrs.getValue("color");
        if (color != null) {
            task.setColor(ColorValueParser.parseString(color));
        }

        String fixedStart = attrs.getValue("fixed-start");
        if ("true".equals(fixedStart)) {
            myContext.addTaskWithLegacyFixedStart(task);
        }

        String third = attrs.getValue("thirdDate");
        if (third != null) {
            task.setThirdDate(GanttCalendar.parseXMLDate(third));
        }
        String thirdConstraint = attrs.getValue("thirdDate-constraint");
        if (thirdConstraint != null) {
            try {
                task.setThirdDateConstraint(Integer.parseInt(thirdConstraint));
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "Failed to parse the value '"
                                + thirdConstraint
                                + "' of attribute 'thirdDate-constraint' of tag <task>",
                        e);
            }
        }

        String webLink_enc = attrs.getValue("webLink");
        String webLink = webLink_enc;
        if (webLink_enc != null)
            try {
                webLink = URLDecoder.decode(webLink_enc, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                if (!GPLogger.log(e)) {
                    e.printStackTrace(System.err);
                }
            }
        if (webLink != null) {
            task.setWebLink(webLink);
        }

        task.setExpand(Boolean.parseBoolean(attrs.getValue("expand")));

        String shape = attrs.getValue("shape");
        if (shape != null) {
            java.util.StringTokenizer st1 = new java.util.StringTokenizer(
                    shape, ",");
            int[] array = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            String token = "";
            int count = 0;
            while (st1.hasMoreTokens()) {
                token = st1.nextToken();
                array[count] = (new Integer(token)).intValue();
                count++;
            }
            task.setShape(new ShapePaint(4, 4, array, Color.white, task
                    .getColor()));
        }

        getManager().registerTask(task);
        TaskContainmentHierarchyFacade taskHierarchy = getManager()
                .getTaskHierarchy();
        myContext.setTaskID(task.getTaskID());
        Task lastTask = myStack.isEmpty() ? taskHierarchy.getRootTask()
                : myStack.peek();
        taskHierarchy.move(task, lastTask);
        myStack.push(task);
    }

    private TaskManager getManager() {
        return myManager;
    }

    private final ParsingContext myContext;

    private final TaskManager myManager;

    private final Stack<GanttTask> myStack = new Stack<GanttTask>();
}
