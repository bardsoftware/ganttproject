/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.io;

import biz.ganttproject.core.time.CalendarFactory;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.GanttGraphicArea;
import net.sourceforge.ganttproject.GanttPreviousState;
import net.sourceforge.ganttproject.GanttResourcePanel;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleSet;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class GanttXMLSaver extends SaverBase implements GPSaver {

  private static final String VERSION = GPVersion.getCurrentVersionNumber();

  private final IGanttProject myProject;

  private final UIFacade myUIFacade;

  private GanttGraphicArea area;

  public GanttXMLSaver(IGanttProject project) {
    this(project, null, null, null, null);
  }

  public GanttXMLSaver(IGanttProject project, TaskTreeUIFacade taskTree, GanttResourcePanel peop, GanttGraphicArea area,
                       UIFacade uiFacade) {
    this.area = area;
    myProject = project;
    myUIFacade = uiFacade;
  }

  @Override
  public void save(OutputStream stream) throws IOException {
    try {
      AttributesImpl attrs = new AttributesImpl();
      StreamResult result = new StreamResult(stream);
      TransformerHandler handler = createHandler(result);
      handler.startDocument();
      addAttribute("name", getProject().getProjectName(), attrs);
      addAttribute("company", getProject().getOrganization(), attrs);
      addAttribute("webLink", getProject().getWebLink(), attrs);
      if (area != null) {
        addAttribute("view-date", CalendarFactory.createGanttCalendar(area.getStartDate()).toXMLString(), attrs);
      }
      if (myUIFacade != null) {
        addAttribute("view-index", "" + myUIFacade.getViewIndex(), attrs);
        // TODO for GP 2.0: move view configurations into <view> tag (see
        // ViewSaver)
        addAttribute("gantt-divider-location", "" + myUIFacade.getGanttDividerLocation(), attrs);
        addAttribute("resource-divider-location", "" + myUIFacade.getResourceDividerLocation(), attrs);
      }
      addAttribute("version", VERSION, attrs);
      addAttribute("locale", GanttLanguage.getInstance().getLocale().toString(), attrs);
      startElement("project", attrs, handler);
      //
      // See https://bugs.openjdk.java.net/browse/JDK-8133452
      if (getProject().getDescription() != null) {
        String projectDescription = getProject().getDescription().replace("\\r\\n", "\\n");
        cdataElement("description", projectDescription, attrs, handler);
      }

      saveViews(handler);
      emptyComment(handler);
      saveCalendar(handler);
      saveTasks(handler);
      saveResources(handler);
      saveAssignments(handler);
      saveVacations(handler);
      saveHistory(handler);
      saveRoles(handler);
      endElement("project", handler);
      handler.endDocument();

      stream.close();
    } catch (Throwable e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
      IOException propagatedException = new IOException("Failed to save the project file");
      propagatedException.initCause(e);
      throw propagatedException;
    }
  }

  private void saveHistory(TransformerHandler handler) throws SAXException, ParserConfigurationException, IOException {
    List<GanttPreviousState> history = myProject.getBaselines();
    new HistorySaver().save(history, handler);
  }

  private void saveVacations(TransformerHandler handler) throws SAXException {
    new VacationSaver().save(getProject(), handler);
  }

  private void saveResources(TransformerHandler handler) throws SAXException {
    new ResourceSaver().save(getProject(), handler);
  }

  private void saveViews(TransformerHandler handler) throws SAXException {
    if (getUIFacade() != null) {
      new ViewSaver().save(getUIFacade(), handler);
    }
  }

  private void saveCalendar(TransformerHandler handler) throws SAXException {
    new CalendarSaver().save(getProject(), handler);
  }

  private void saveTasks(TransformerHandler handler) throws SAXException, IOException {
    new TaskSaver().save(getProject(), handler);
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
