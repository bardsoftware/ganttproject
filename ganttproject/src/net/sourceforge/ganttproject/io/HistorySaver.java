package net.sourceforge.ganttproject.io;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttPreviousStateTask;

class HistorySaver extends SaverBase {

    void save(List/*<GanttPreviousState*/<GanttPreviousState> history, TransformerHandler handler) throws SAXException, ParserConfigurationException, IOException {
        AttributesImpl attrs = new AttributesImpl();
        startElement("previous", handler);
        for (int i=0; i<history.size(); i++) {
            final GanttPreviousState nextState = history.get(i);
            final List/*<GanttPreviousStateTask>*/<GanttPreviousStateTask> stateTasks = nextState.load();
            addAttribute("name", nextState.getName(), attrs);
            startElement("previous-tasks", attrs, handler);
            // ArrayList list =
            // ((GanttPreviousState)previous.get(i)).getTasks();
            for (int j=0; j<stateTasks.size(); j++) {
                GanttPreviousStateTask task = stateTasks.get(j);
                addAttribute("id", task.getId(), attrs);
                addAttribute("start", task.getStart().toXMLString(), attrs);
                addAttribute("duration", task.getDuration(), attrs);
                addAttribute("meeting", task.isMilestone(), attrs);
                addAttribute("super", task.hasNested(), attrs);
                emptyElement("previous-task", attrs, handler);

            }
            endElement("previous-tasks", handler);
        }
        endElement("previous", handler);
    }

}
