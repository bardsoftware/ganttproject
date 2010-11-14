package net.sourceforge.ganttproject.io;

import java.util.List;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;

class AssignmentSaver extends SaverBase {
    void save(IGanttProject project, TransformerHandler handler) throws SAXException {
        startElement("allocations", handler);
        List<HumanResource> resources = project.getHumanResourceManager().getResources();
        for (int i=0; i<resources.size(); i++) {
        	HumanResource resource = resources.get(i);
            ResourceAssignment[] assignments = resource.getAssignments();
            for (int j=0; j<assignments.length; j++) {
                saveAssignment(handler, assignments[j]);
            }
        }
        endElement("allocations", handler);
    }

    void saveAssignment(TransformerHandler handler, ResourceAssignment next) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        Role roleForAssignment = next.getRoleForAssignment();
        if (roleForAssignment == null) {
            roleForAssignment = next.getResource().getRole();
        }
        addAttribute("task-id", String.valueOf(next.getTask().getTaskID()), attrs);
        addAttribute("resource-id", String.valueOf(next.getResource().getId()), attrs);
        addAttribute("function", roleForAssignment.getPersistentID(), attrs);
        addAttribute("responsible", String.valueOf(next.isCoordinator()), attrs);
        addAttribute("load", String.valueOf(next.getLoad()), attrs);
        emptyElement("allocation", attrs, handler);
    }
}
