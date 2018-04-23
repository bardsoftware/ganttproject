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

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.GPCalendarListener;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.StringOption;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task.Priority;
import net.sourceforge.ganttproject.task.algorithm.AlgorithmCollection;
import net.sourceforge.ganttproject.task.algorithm.DependencyGraph;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyCollection;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.event.TaskListener;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * @author bard
 */
public interface TaskManager {
  abstract class TaskBuilder {
    String myName;
    Integer myId;
    Date myStartDate;
    TimeDuration myDuration;
    Color myColor;
    Task myPrevSibling;
    Boolean isExpanded;
    Task myParent;
    boolean isLegacyMilestone;
    Date myEndDate;
    String myNotes;
    String myWebLink;
    Integer myCompletion;
    Priority myPriority;
    Task myPrototype;
    BigDecimal myCost;

    public TaskBuilder withColor(Color color) {
      myColor = color;
      return this;
    }

    public TaskBuilder withCompletion(int pctgCompletion) {
      myCompletion = pctgCompletion;
      return this;
    }

    public TaskBuilder withDuration(TimeDuration duration) {
      myDuration = duration;
      return this;
    }

    public TaskBuilder withEndDate(Date date) {
      myEndDate = date;
      return this;
    }

    public TaskBuilder withExpansionState(boolean isExpanded) {
      this.isExpanded = isExpanded;
      return this;
    }

    public TaskBuilder withId(int id) {
      myId = id;
      return this;
    }

    public TaskBuilder withLegacyMilestone() {
      isLegacyMilestone = true;
      return this;
    }

    public TaskBuilder withName(String name) {
      myName = name;
      return this;
    }

    public TaskBuilder withNotes(String notes) {
      myNotes = notes;
      return this;
    }

    public TaskBuilder withParent(Task parent) {
      myParent = parent;
      return this;
    }
    public TaskBuilder withPrevSibling(Task sibling) {
      myPrevSibling = sibling;
      return this;
    }

    public TaskBuilder withPriority(Priority priority) {
      myPriority = priority;
      return this;
    }

    public TaskBuilder withPrototype(Task prototype) {
      myPrototype = prototype;
      return this;
    }

    public TaskBuilder withStartDate(Date startDate) {
      myStartDate = startDate;
      return this;
    }

    public TaskBuilder withWebLink(String value) {
      myWebLink = value;
      return this;
    }

    public TaskBuilder withCost(BigDecimal value) {
      myCost = value;
      return this;
    }

    public abstract Task build();
  }

  public TaskBuilder newTaskBuilder();

  Task[] getTasks();

  public Task getRootTask();

  public GanttTask getTask(int taskId);

  public void registerTask(Task task);

  public GanttTask createTask();

  @Deprecated
  public GanttTask createTask(int taskId);


  String encode(TimeDuration duration);

  TimeDuration createLength(String lengthAsString);

  public TimeDuration createLength(long length);

  TimeDuration createLength(TimeUnit unit, float length);

  public TimeDuration createLength(TimeUnit timeUnit, Date startDate, Date endDate);

  Date shift(Date original, TimeDuration duration);

  TaskDependencyCollection getDependencyCollection();

  AlgorithmCollection getAlgorithmCollection();

  TaskDependencyConstraint createConstraint(TaskDependencyConstraint.Type constraintType);

  GPCalendarCalc getCalendar();

  TaskContainmentHierarchyFacade getTaskHierarchy();

  void addTaskListener(TaskListener listener);

  public class Access {
    public static TaskManager newInstance(TaskContainmentHierarchyFacade.Factory containmentFacadeFactory,
        TaskManagerConfig config) {
      return new TaskManagerImpl(containmentFacadeFactory, config);
    }
  }

  public TimeDuration getProjectLength();

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

  StringOption getTaskCopyNamePrefixOption();

  ColorOption getTaskDefaultColorOption();

  EnumerationOption getDependencyHardnessOption();

  void setZeroMilestones(Boolean b);

  Boolean isZeroMilestones();

  DependencyGraph getDependencyGraph();

  ProjectEventListener getProjectListener();

  GPCalendarListener getCalendarListener();
}
