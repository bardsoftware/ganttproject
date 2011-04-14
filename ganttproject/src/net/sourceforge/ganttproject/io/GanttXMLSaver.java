/***************************************************************************
 * GanttXMLSaver.java  -  description
 * -------------------
 * begin                : feb 2003
 * copyright            : (C) 2002 by Thomas Alexandre
 * email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttGraphicArea;
import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.GanttResourcePanel;
import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleSet;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Classe for save the project in a XML file
 */
public class GanttXMLSaver extends SaverBase implements GPSaver {

    private static final String VERSION = "2.0";

    private final IGanttProject myProject;

    private final UIFacade myUIFacade;

    private GanttTree2 tree;

    private GanttGraphicArea area;

    /** The constructor */
    public GanttXMLSaver(IGanttProject project, GanttTree2 tree,
            GanttResourcePanel peop, GanttGraphicArea area, UIFacade uiFacade) {
        this.tree = tree;
        this.area = area;
        myProject = project;
        myUIFacade = uiFacade;
    }

    public void save(OutputStream stream) throws IOException {
        try {
            AttributesImpl attrs = new AttributesImpl();
            StreamResult result = new StreamResult(stream);
            TransformerHandler handler = createHandler(result);
            handler.startDocument();
            addAttribute("name", getProject().getProjectName(), attrs);
            addAttribute("company", getProject().getOrganization(), attrs);
            addAttribute("webLink", getProject().getWebLink(), attrs);
            addAttribute("view-date", new GanttCalendar(area
                    .getStartDate()).toXMLString(), attrs);
            addAttribute("view-index", "" + myUIFacade.getViewIndex(), attrs);
            //TODO for GP 2.0: move view configurations into <view> tag (see ViewSaver)
            addAttribute("gantt-divider-location", ""
                    + myUIFacade.getGanttDividerLocation(), attrs);
            addAttribute("resource-divider-location", ""
                    + myUIFacade.getResourceDividerLocation(), attrs);
            addAttribute("version", VERSION, attrs);
            startElement("project", attrs, handler);
            //
            cdataElement("description", getProject().getDescription(), attrs, handler);

            saveViews(handler);
            emptyComment(handler);
            saveCalendar(handler);
            saveTasks(handler);
            saveResources(handler);
            saveAssignments(handler);
            saveVacations(handler);
            saveGanttChartView(handler);
            saveHistory(handler);
            saveRoles(handler);
            endElement("project", handler);
            handler.endDocument();

            stream.close();
        } catch (Throwable e) {
            if (!GPLogger.log(e)) {
                e.printStackTrace(System.err);
            }
            IOException propagatedException  = new IOException("Failed to save the project file");
            propagatedException.initCause(e);
            throw propagatedException;
        }
    }

    private void saveHistory(TransformerHandler handler) throws SAXException, ParserConfigurationException, IOException {
        List<GanttPreviousState> history = ((GanttProject) myProject).getBaselines();
        new HistorySaver().save(history, handler);
    }

    private void saveGanttChartView(TransformerHandler handler) throws SAXException {
        new GanttChartViewSaver().save(tree.getTreeTable(), handler);
    }

    private void saveVacations(TransformerHandler handler) throws SAXException {
        new VacationSaver().save(getProject(), handler);
    }

    private void saveResources(TransformerHandler handler) throws SAXException {
        new ResourceSaver().save(getProject(), handler);
    }

    private void saveViews(TransformerHandler handler) throws SAXException {
        new ViewSaver().save(getUIFacade(), handler);
    }

    private void saveCalendar(TransformerHandler handler) throws SAXException {
        new CalendarSaver().save(getProject(), handler);
    }

    private void saveTasks(TransformerHandler handler) throws SAXException, IOException {
        new TaskSaver().save(getProject(), handler, area.getTaskColor());
    }

    private void saveAssignments(TransformerHandler handler) throws SAXException {
        new AssignmentSaver().save(getProject(), handler);
    }
    private void saveRoles(TransformerHandler handler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        RoleManager roleManager = getProject().getRoleManager();
        RoleSet[] roleSets = roleManager.getRoleSets();
        for (int i = 0; i < roleSets.length; i++) {
            RoleSet next = roleSets[i];
            if (next.isEnabled()) {
                addAttribute("roleset-name", next.getName(), attrs);
                emptyElement("roles", attrs, handler);
            }
        }
        //
        RoleSet projectRoleSet = roleManager.getProjectRoleSet();
        if (!projectRoleSet.isEmpty()) {
            startElement("roles", attrs, handler);
            Role[] projectRoles = projectRoleSet.getRoles();
            for (int i = 0; i < projectRoles.length; i++) {
                Role next = projectRoles[i];
                addAttribute("id", next.getPersistentID(), attrs);
                addAttribute("name", next.getName(), attrs);
                emptyElement("role", attrs, handler);
            }
            endElement("roles", handler);
        }
    }


    IGanttProject getProject() {
        return myProject;
    }

    UIFacade getUIFacade() {
        return myUIFacade;
    }
}
