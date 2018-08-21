/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Alexandre Thomas, Dmitry Barashev

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

import biz.ganttproject.core.chart.render.ShapePaint;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeDuration;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.task.dependency.TaskDependencySlice;

import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Project task definition
 *
 * @author bard
 */
public interface Task extends MutableTask {
  /** Available task priorities */
  public enum Priority {
    LOWEST("3"), LOW("0"), NORMAL("1"), HIGH("2"), HIGHEST("4");

    private final String myPersistentValue;

    private Priority(String persistentValue) {
      myPersistentValue = persistentValue;
    }

    /**
     * @return the Priority value for the given integer value, or
     *         DEFAULT_PRIORITY if unknown
     */
    public static Priority getPriority(int value) {
      for (Task.Priority p : Task.Priority.values()) {
        if (p.ordinal() == value) {
          return p;
        }
      }
      return DEFAULT_PRIORITY;
    }

    public String getPersistentValue() {
      return myPersistentValue;
    }

    /** @return the priority as a lower-case String */
    public String getLowerString() {
      return this.toString().toLowerCase();
    }

    /** @return the key to get the I18n value for the priority */
    public String getI18nKey() {
      return "priority." + getLowerString();
    }

    /**
     * @return the path to the icon representing the priority
     */
    public String getIconPath() {
      return "/icons/task_" + getLowerString() + ".gif";
    }

    public static Priority fromPersistentValue(String priority) {
      for (Priority p : values()) {
        if (p.getPersistentValue().equals(priority)) {
          return p;
        }
      }
      return Priority.NORMAL;
    }
  }

  /** Default priority (for new tasks) */
  public static final Priority DEFAULT_PRIORITY = Priority.NORMAL;

  public static interface Cost {
    BigDecimal getValue();
    BigDecimal getManualValue();
    BigDecimal getCalculatedValue();
    void setValue(Cost copy);
    void setValue(BigDecimal value);
    boolean isCalculated();
    void setCalculated(boolean calculated);
  }

  Cost getCost();

  TaskMutator createMutator();

  TaskMutator createMutatorFixingDuration();

  // main properties
  int getTaskID();

  String getName();

  boolean isMilestone();

  Priority getPriority();

  List<TaskActivity> getActivities();

  GanttCalendar getStart();

  GanttCalendar getDisplayEnd();
  GanttCalendar getEnd();

  TimeDuration getDuration();

  TimeDuration translateDuration(TimeDuration duration);

  int getCompletionPercentage();

  ShapePaint getShape();

  /**
   * @return a color representing this Task (could be a custom color, milestone
   *         color, super task color or default color)
   */
  Color getColor();

  String getNotes();

  boolean getExpand();

  // HumanResource[] getAssignedHumanResources();
  ResourceAssignment[] getAssignments();

  TaskDependencySlice getDependencies();

  TaskDependencySlice getDependenciesAsDependant();

  TaskDependencySlice getDependenciesAsDependee();

  ResourceAssignmentCollection getAssignmentCollection();

  //
  Task getSupertask();

  Task[] getNestedTasks();

  void move(Task targetSupertask);

  void move(Task targetSupertask, int position);

  void delete();

  TaskManager getManager();

  Task unpluggedClone();

  CustomColumnsValues getCustomValues();

  boolean isCritical();

  GanttCalendar getThird();

  void applyThirdDateConstraint();

  int getThirdDateConstraint();

  void setThirdDate(GanttCalendar thirdDate);

  void setThirdDateConstraint(int dateConstraint);

  TaskInfo getTaskInfo();

  boolean isProjectTask();

  boolean isSupertask();

  List<Document> getAttachments();
}
