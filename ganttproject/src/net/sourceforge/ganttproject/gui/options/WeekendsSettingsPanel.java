/*
GanttProject is an opensource project management tool.
Copyright (C) 2010-2011 GanttProject Team

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
package net.sourceforge.ganttproject.gui.options;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.projectwizard.I18N;
import net.sourceforge.ganttproject.gui.projectwizard.WeekendConfigurationPage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

/**
 * Panel to edit the weekend settings
 *
 * @author Maarten Bezemer
 */
public class WeekendsSettingsPanel extends GeneralOptionPanel {

  private final IGanttProject project;

  private WeekendConfigurationPage weekendConfigurationPanel;

  private GPCalendarCalc calendar;

  public WeekendsSettingsPanel(IGanttProject project, UIFacade uiFacade) {
    super(uiFacade, language.getCorrectedLabel("weekends"), language.getText("settingsWeekends"));

    this.project = project;
    calendar = new WeekendCalendarImpl();

    weekendConfigurationPanel = null;
  }

  @Override
  public boolean applyChanges(boolean askForApply) {
    weekendConfigurationPanel.setActive(false);
    GPCalendarCalc projectCalendar = project.getActiveCalendar();
    boolean hasChange = weekendConfigurationPanel.isChanged();
    for (int i = 1; !hasChange && i < 8; i++) {
      if (calendar.getWeekDayType(i) != projectCalendar.getWeekDayType(i)) {
        hasChange = true;
      }
    }
    for (int i = 1; i < 8; i++) {
      projectCalendar.setWeekDayType(i, calendar.getWeekDayType(i));
    }
    if (hasChange) {
      projectCalendar.setBaseCalendarID(calendar.getBaseCalendarID());
      projectCalendar.setPublicHolidays(calendar.getPublicHolidays());
      projectCalendar.setOnlyShowWeekends(calendar.getOnlyShowWeekends());
      try {
        TaskManager taskManager = project.getTaskManager();
        taskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
        taskManager.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().adjustNestedTasks(taskManager.getRootTask());
      } catch (TaskDependencyException e) {
        GPLogger.log(e);
      }
    }
    return hasChange;
  }

  // TODO It would be nicer to just update the checkboxes,
  // but WeekendConfigurationPage does not allow it ATM
  @Override
  public void initialize() {
    if (weekendConfigurationPanel != null) {
      vb.remove(weekendConfigurationPanel.getComponent());
    }

    // Make a copy of the WeekDayTypes
    calendar = project.getActiveCalendar().copy();
    weekendConfigurationPanel = new WeekendConfigurationPage(calendar, new I18N(), getUIFacade());
    vb.add(weekendConfigurationPanel.getComponent());
  }
}
