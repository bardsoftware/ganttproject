/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2010 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.font.Fonts;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.time.TimeUnitStack;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;

public class GanttProjectImpl implements IGanttProject {
    private String myProjectName;
    private String myDescription;
    private String myOrganization;
    private String myWebLink;
    private final TaskManager myTaskManager;
    private final HumanResourceManager myResourceManager;
    private final TaskManagerConfigImpl myTaskManagerConfig;
    private Document myDocument;
    private final List<ProjectEventListener> myListeners = new ArrayList<ProjectEventListener>();
    private UIConfiguration myUIConfiguration;
    private final CustomColumnsManager myTaskCustomColumnManager;
    private final CustomColumnsStorage myTaskCustomColumnStorage;
    private final List<GanttPreviousState> myBaselines = new ArrayList<GanttPreviousState>();

    public GanttProjectImpl() {
        myResourceManager = new HumanResourceManager(RoleManager.Access.getInstance().getDefaultRole());
        myTaskManagerConfig = new TaskManagerConfigImpl(myResourceManager, GanttLanguage.getInstance());
        myTaskManager = TaskManager.Access.newInstance(null, myTaskManagerConfig);
        myUIConfiguration = new UIConfiguration(Fonts.DEFAULT_MENU_FONT, Fonts.DEFAULT_CHART_FONT, Color.BLUE, true);
        myTaskCustomColumnStorage = new CustomColumnsStorage();
        myTaskCustomColumnManager = new CustomColumnsManager(myTaskCustomColumnStorage);
    }
    public String getProjectName() {
        return myProjectName;
    }

    public void setProjectName(String projectName) {
        myProjectName = projectName;
    }

    public String getDescription() {
        return myDescription;
    }

    public void setDescription(String description) {
        myDescription = description;
    }

    public String getOrganization() {
        return myOrganization;
    }

    public void setOrganization(String organization) {
        myOrganization = organization;
    }

    public String getWebLink() {
        return myWebLink;
    }

    public void setWebLink(String webLink) {
        myWebLink = webLink;
    }

    public Task newTask() {
        Task result = getTaskManager().createTask();
        getTaskManager().getTaskHierarchy().move(result,getTaskManager().getRootTask());
        return result;
    }

    public GanttLanguage getI18n() {
        return GanttLanguage.getInstance();
    }

    public UIConfiguration getUIConfiguration() {
        return myUIConfiguration;
    }

    public HumanResourceManager getHumanResourceManager() {
        return myResourceManager;
    }

    public RoleManager getRoleManager() {
        return RoleManager.Access.getInstance();
    }

    public TaskManager getTaskManager() {
        return myTaskManager;
    }

    public TaskContainmentHierarchyFacade getTaskContainment() {
        return getTaskManager().getTaskHierarchy();
    }

    public GPCalendar getActiveCalendar() {
        return myTaskManagerConfig.getCalendar();
    }

    public TimeUnitStack getTimeUnitStack() {
        return myTaskManagerConfig.getTimeUnitStack();
    }

    public void setModified() {
        // TODO Auto-generated method stub
    }

    public void setModified(boolean modified) {
        // TODO Auto-generated method stub
    }

    public void close() {
        // TODO Auto-generated method stub
    }

    public Document getDocument() {
        return myDocument;
    }

    public void setDocument(Document document) {
        myDocument = document;
    }

    public void addProjectEventListener(ProjectEventListener listener) {
        myListeners.add(listener);
    }

    public void removeProjectEventListener(ProjectEventListener listener) {
        myListeners.remove(listener);
    }

    public boolean isModified() {
        // TODO Auto-generated method stub
        return false;
    }

    public void open(Document document) throws IOException {
        // TODO Auto-generated method stub
    }

    public DocumentManager getDocumentManager() {
        // TODO Auto-generated method stub
        return null;
    }
    public CustomPropertyManager getResourceCustomPropertyManager() {
        return myResourceManager;
    };

    private static class TaskManagerConfigImpl implements TaskManagerConfig {
        private final HumanResourceManager myResourceManager;
        private final GPTimeUnitStack myTimeUnitStack;
        private final WeekendCalendarImpl myCalendar;

        private TaskManagerConfigImpl(HumanResourceManager resourceManager, GanttLanguage i18n) {
            myResourceManager = resourceManager;
            myTimeUnitStack = new GPTimeUnitStack(i18n);
            myCalendar = new WeekendCalendarImpl();
        }

        public Color getDefaultColor() {
            return Color.BLUE;
        }

        public GPCalendar getCalendar() {
            return myCalendar;
        }

        public TimeUnitStack getTimeUnitStack() {
            return myTimeUnitStack;
        }

        public HumanResourceManager getResourceManager() {
            return myResourceManager;
        }
        public URL getProjectDocumentURL() {
            return null;
        }
    }

    public CustomColumnsManager getTaskCustomColumnManager() {
        return myTaskCustomColumnManager;
    }

    public CustomColumnsStorage getCustomColumnsStorage() {
        return myTaskCustomColumnStorage;
    }

    public List<GanttPreviousState> getBaselines() {
        return myBaselines;
    }

    public void repaintResourcePanel() {
        // TODO Auto-generated method stub
    }
}
