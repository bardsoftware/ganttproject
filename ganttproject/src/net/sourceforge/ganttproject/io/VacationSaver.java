package net.sourceforge.ganttproject.io;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ProjectResource;

class VacationSaver extends SaverBase {

    void save(IGanttProject project, TransformerHandler handler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        startElement("vacations", handler);
        ProjectResource[] resources = project.getHumanResourceManager().getResourcesArray();
        for (int i = 0; i < resources.length; i++) {
            HumanResource p = (HumanResource) resources[i];
            if (p.getDaysOff() != null)
                for (int j = 0; j < p.getDaysOff().size(); j++) {
                    GanttDaysOff gdo = (GanttDaysOff) p.getDaysOff()
                            .getElementAt(j);
                    addAttribute("start", gdo.getStart().toXMLString(), attrs);
                    addAttribute("end", gdo.getFinish().toXMLString(), attrs);
                    addAttribute("resourceid", p.getId(), attrs);
                    emptyElement("vacation", attrs, handler);
                }
        }
        endElement("vacations", handler);
    }

}
