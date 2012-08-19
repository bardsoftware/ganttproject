/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 Dmitry Barashev, GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import biz.ganttproject.core.time.TimeUnitStack;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

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
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;

public class GanttProjectImpl implements IGanttProject {
  private static final GanttLanguage language = GanttLanguage.getInstance();
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
  private final List<GanttPreviousState> myBaselines = new ArrayList<GanttPreviousState>();

  public GanttProjectImpl() {
    myResourceManager = new HumanResourceManager(RoleManager.Access.getInstance().getDefaultRole(),
        new CustomColumnsManager());
    myTaskManagerConfig = new TaskManagerConfigImpl(myResourceManager, GanttLanguage.getInstance());
    myTaskManager = TaskManager.Access.newInstance(null, myTaskManagerConfig);
    myUIConfiguration = new UIConfiguration(Fonts.DEFAULT_MENU_FONT, Fonts.DEFAULT_CHART_FONT, Color.BLUE, true);
    myTaskCustomColumnManager = new CustomColumnsManager();
  }

  @Override
  public String getProjectName() {
    return myProjectName;
  }

  @Override
  public void setProjectName(String projectName) {
    myProjectName = projectName;
  }

  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  public void setDescription(String description) {
    myDescription = description;
  }

  @Override
  public String getOrganization() {
    return myOrganization;
  }

  @Override
  public void setOrganization(String organization) {
    myOrganization = organization;
  }

  @Override
  public String getWebLink() {
    return myWebLink;
  }

  @Override
  public void setWebLink(String webLink) {
    myWebLink = webLink;
  }

  public Task newTask() {
    Task result = getTaskManager().createTask();
    getTaskManager().getTaskHierarchy().move(result, getTaskManager().getRootTask());
    return result;
  }

  public GanttLanguage getLanguage() {
    return language;
  }

  @Override
  public UIConfiguration getUIConfiguration() {
    return myUIConfiguration;
  }

  @Override
  public HumanResourceManager getHumanResourceManager() {
    return myResourceManager;
  }

  @Override
  public RoleManager getRoleManager() {
    return RoleManager.Access.getInstance();
  }

  @Override
  public TaskManager getTaskManager() {
    return myTaskManager;
  }

  @Override
  public TaskContainmentHierarchyFacade getTaskContainment() {
    return getTaskManager().getTaskHierarchy();
  }

  @Override
  public GPCalendar getActiveCalendar() {
    return myTaskManagerConfig.getCalendar();
  }

  @Override
  public TimeUnitStack getTimeUnitStack() {
    return myTaskManagerConfig.getTimeUnitStack();
  }

  @Override
  public void setModified() {
    // TODO Auto-generated method stub
  }

  @Override
  public void setModified(boolean modified) {
    // TODO Auto-generated method stub
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
  }

  @Override
  public Document getDocument() {
    return myDocument;
  }

  @Override
  public void setDocument(Document document) {
    myDocument = document;
  }

  @Override
  public void addProjectEventListener(ProjectEventListener listener) {
    myListeners.add(listener);
  }

  @Override
  public void removeProjectEventListener(ProjectEventListener listener) {
    myListeners.remove(listener);
  }

  @Override
  public boolean isModified() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void open(Document document) throws IOException {
    // TODO Auto-generated method stub
  }

  @Override
  public DocumentManager getDocumentManager() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CustomPropertyManager getResourceCustomPropertyManager() {
    return myResourceManager.getCustomPropertyManager();
  };

  private static class TaskManagerConfigImpl implements TaskManagerConfig {
    private final HumanResourceManager myResourceManager;
    private final GPTimeUnitStack myTimeUnitStack;
    private final WeekendCalendarImpl myCalendar;

    private TaskManagerConfigImpl(HumanResourceManager resourceManager, GanttLanguage i18n) {
      myResourceManager = resourceManager;
      myTimeUnitStack = new GPTimeUnitStack();
      myCalendar = new WeekendCalendarImpl();
    }

    @Override
    public Color getDefaultColor() {
      return Color.BLUE;
    }

    @Override
    public GPCalendar getCalendar() {
      return myCalendar;
    }

    @Override
    public TimeUnitStack getTimeUnitStack() {
      return myTimeUnitStack;
    }

    @Override
    public HumanResourceManager getResourceManager() {
      return myResourceManager;
    }

    @Override
    public URL getProjectDocumentURL() {
      return null;
    }
  }

  @Override
  public CustomPropertyManager getTaskCustomColumnManager() {
    return myTaskCustomColumnManager;
  }

  @Override
  public List<GanttPreviousState> getBaselines() {
    return myBaselines;
  }

  public void repaintResourcePanel() {
    // TODO Auto-generated method stub
  }
}
