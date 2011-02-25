package net.sourceforge.ganttproject.io;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttPreviousStateTask;

public class HistorySaver extends SaverBase {

    void save(List<GanttPreviousState> history, TransformerHandler handler) throws SAXException, ParserConfigurationException, IOException {
        startElement("previous", handler);
        for (GanttPreviousState baseline : history) {
            saveBaseline(baseline, handler);
        }
        endElement("previous", handler);
    }

    public void saveBaseline(GanttPreviousState nextState, TransformerHandler handler) throws SAXException {
        saveBaseline(nextState.getName(), nextState.load(), handler);
    }

    public void saveBaseline(String name, List<GanttPreviousStateTask> tasks, TransformerHandler handler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("name", name, attrs);
        startElement("previous-tasks", attrs, handler);
        for (GanttPreviousStateTask task : tasks) {
            addAttribute("id", task.getId(), attrs);
            addAttribute("start", task.getStart().toXMLString(), attrs);
            addAttribute("duration", task.getDuration(), attrs);
            addAttribute("meeting", task.isMilestone(), attrs);
            addAttribute("super", task.hasNested(), attrs);
            emptyElement("previous-task", attrs, handler);
        }
        endElement("previous-tasks", handler);

    }
}
