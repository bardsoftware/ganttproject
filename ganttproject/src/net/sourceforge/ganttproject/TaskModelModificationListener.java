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
package net.sourceforge.ganttproject;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;

public class TaskModelModificationListener extends TaskListenerAdapter {
  private IGanttProject myGanttProject;
  private UIFacade myUiFacade;

  TaskModelModificationListener(IGanttProject ganttProject, UIFacade uiFacade) {
    myGanttProject = ganttProject;
    myUiFacade = uiFacade;
  }

  @Override
  public void taskScheduleChanged(TaskScheduleEvent e) {
    myGanttProject.setModified();
    myGanttProject.getTaskManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run(e.getTask());
  }

  @Override
  public void dependencyAdded(TaskDependencyEvent e) {
    myGanttProject.setModified();
  }

  @Override
  public void dependencyRemoved(TaskDependencyEvent e) {
    myGanttProject.setModified();
  }

  @Override
  public void dependencyChanged(TaskDependencyEvent e) {
    myGanttProject.setModified();
  }

  @Override
  public void taskAdded(TaskHierarchyEvent e) {
    myGanttProject.setModified();
    myUiFacade.setViewIndex(UIFacade.GANTT_INDEX);
    myGanttProject.getTaskManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run(e.getTask());
    myUiFacade.refresh();
  }

  @Override
  public void taskRemoved(TaskHierarchyEvent e) {
    myGanttProject.setModified();
    myGanttProject.getTaskManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run();
  }

  @Override
  public void taskMoved(TaskHierarchyEvent e) {
    myGanttProject.setModified();
  }

  @Override
  public void taskPropertiesChanged(TaskPropertyEvent e) {
    myGanttProject.setModified();
  }

  @Override
  public void taskProgressChanged(TaskPropertyEvent e) {
    myGanttProject.setModified();
    e.getTask().getManager().getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm().run(e.getTask());
  }
}
