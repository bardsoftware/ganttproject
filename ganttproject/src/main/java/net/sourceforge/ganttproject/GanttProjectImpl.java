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

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.GPCalendarListener;
import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.time.TimeUnitStack;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import com.google.common.base.Strings;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
  private final WeekendCalendarImpl myCalendar = new WeekendCalendarImpl();

  public GanttProjectImpl() {
    myResourceManager = new HumanResourceManager(RoleManager.Access.getInstance().getDefaultRole(),
        new CustomColumnsManager());
    myTaskManagerConfig = new TaskManagerConfigImpl(myResourceManager, myCalendar, GanttLanguage.getInstance());
    myTaskManager = TaskManager.Access.newInstance(null, myTaskManagerConfig);
    myUIConfiguration = new UIConfiguration(Color.BLUE, true);
    myTaskCustomColumnManager = new CustomColumnsManager();
    myCalendar.addListener(new GPCalendarListener() {
      @Override
      public void onCalendarChange() {
        setModified();
      }
    });
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
    return Strings.nullToEmpty(myDescription);
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
  public GPCalendarCalc getActiveCalendar() {
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
  }

  ;

  private static Color DEFAULT_TASK_COLOR = new Color(140, 182, 206);

  private static class TaskManagerConfigImpl implements TaskManagerConfig {
    private final HumanResourceManager myResourceManager;
    private final GPTimeUnitStack myTimeUnitStack;
    private final GPCalendarCalc myCalendar;
    private final ColorOption myDefaultTaskColorOption;

    private TaskManagerConfigImpl(HumanResourceManager resourceManager, GPCalendarCalc calendar, GanttLanguage i18n) {
      myResourceManager = resourceManager;
      myTimeUnitStack = new GPTimeUnitStack();
      myCalendar = calendar;
      myDefaultTaskColorOption = new DefaultTaskColorOption(DEFAULT_TASK_COLOR);
    }

    @Override
    public Color getDefaultColor() {
      return myDefaultTaskColorOption.getValue();
    }

    @Override
    public ColorOption getDefaultColorOption() {
      return myDefaultTaskColorOption;
    }

    @Override
    public GPCalendarCalc getCalendar() {
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

    @Override
    public NotificationManager getNotificationManager() {
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


  static class DefaultTaskColorOption extends DefaultColorOption implements GP1XOptionConverter {
    DefaultTaskColorOption() {
      this(DEFAULT_TASK_COLOR);
    }

    private DefaultTaskColorOption(Color defaultColor) {
      super("taskDefaultColor", defaultColor);
    }

    @Override
    public String getTagName() {
      return "colors";
    }

    @Override
    public String getAttributeName() {
      return "tasks";
    }

    @Override
    public void loadValue(String legacyValue) {
      loadPersistentValue(legacyValue);
      commit();
    }
  }


}
