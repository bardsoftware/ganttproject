/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

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
package net.sourceforge.ganttproject.task;

import java.util.Date;
import java.util.Map;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.gui.options.model.EnumerationOption;
import net.sourceforge.ganttproject.gui.options.model.StringOption;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmCollection;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyCollection;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.event.TaskListener;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
public interface TaskManager {
  Task[] getTasks();

  public Task getRootTask();

  void projectOpened();

  public void projectClosed();

  public GanttTask getTask(int taskId);

  public void registerTask(Task task);

  public GanttTask createTask();

  public GanttTask createTask(int taskId);

  String encode(TaskLength duration);

  TaskLength createLength(String lengthAsString);

  public TaskLength createLength(long length);

  TaskLength createLength(TimeUnit unit, float length);

  public TaskLength createLength(TimeUnit timeUnit, Date startDate, Date endDate);

  Date shift(Date original, TaskLength duration);

  TaskDependencyCollection getDependencyCollection();

  AlgorithmCollection getAlgorithmCollection();

  TaskDependencyConstraint createConstraint(int constraintID);

  TaskDependencyConstraint createConstraint(TaskDependencyConstraint.Type constraintType);

  GPCalendar getCalendar();

  TaskContainmentHierarchyFacade getTaskHierarchy();

  void addTaskListener(TaskListener listener);

  public class Access {
    public static TaskManager newInstance(TaskContainmentHierarchyFacade.Factory containmentFacadeFactory,
        TaskManagerConfig config) {
      return new TaskManagerImpl(containmentFacadeFactory, config);
    }
  }

  public TaskLength getProjectLength();

  public int getTaskCount();

  public Date getProjectStart();

  public Date getProjectEnd();

  int getProjectCompletion();

  public TaskManager emptyClone();

  public Map<Task, Task> importData(TaskManager taskManager,
      Map<CustomPropertyDefinition, CustomPropertyDefinition> customPropertyMapping);

  public void importAssignments(TaskManager importedTaskManager, HumanResourceManager hrManager,
      Map<Task, Task> original2importedTask, Map<HumanResource, HumanResource> original2importedResource);

  /**
   * Processes the critical path finding on <code>root</code> tasks.
   * 
   * @param root
   *          The root of the tasks to consider in the critical path finding.
   */
  public void processCriticalPath(Task root);

  public void deleteTask(Task tasktoRemove);

  CustomPropertyManager getCustomPropertyManager();

  StringOption getTaskNamePrefixOption();

  EnumerationOption getDependencyHardnessOption();
}
