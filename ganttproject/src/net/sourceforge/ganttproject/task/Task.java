/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Alexandre Thomas, Dmitry Barashev

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
package net.sourceforge.ganttproject.task;

import java.awt.Color;
import java.util.List;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTaskRelationship;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.shape.ShapePaint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencySlice;

/**
 * Project task denifition
 *
 * @author bard
 */
public interface Task extends MutableTask {
    /**
     * Available task priorities
     */
    public enum Priority {
        LOWEST, LOW, NORMAL, HIGH, HIGHEST;

        /**
         * @return the Priority value for the given integer value, or DEFAULT_PRIORITY if unknown
         */
        public static Priority getPriority(int value) {
            for (Task.Priority p: Task.Priority.values()) {
                if (p.ordinal() == value) {
                    return p;
                }
            }
            return DEFAULT_PRIORITY;
        }

        /**
         * @return the priority as a lower-case String
         */
        public String getLowerString() {
            return this.toString().toLowerCase();
        }

        /**
         * @return the key to get the I18n value for the priority
         */
        public String getI18nKey() {
            return "priority." + getLowerString();
        }

        /**
         * @return the path to the icon representing the priority
         */
        public String getIconPath() {
            return "/icons/task_" + getLowerString() + ".gif";
        }
    }

    /**
     * Default priority (for new tasks)
     */
    public static final Priority DEFAULT_PRIORITY = Priority.NORMAL; 

    TaskMutator createMutator();
    TaskMutator createMutatorFixingDuration();
    // main properties
    int getTaskID();

    String getName();

    boolean isMilestone();

    Priority getPriority();

    TaskActivity[] getActivities();

    GanttCalendar getStart();

    GanttCalendar getEnd();

    TaskLength getDuration();

    TaskLength translateDuration(TaskLength duration);

    int getCompletionPercentage();

    ShapePaint getShape();

    Color getColor();

    String getNotes();

    boolean getExpand();

    //
    // relationships with other entities
    GanttTaskRelationship[] getPredecessors();

    GanttTaskRelationship[] getSuccessors();

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

    void delete();

    TaskManager getManager();

    Task unpluggedClone();

    // Color DEFAULT_COLOR = new Color( 140, 182, 206); not used

    CustomColumnsValues getCustomValues();

    boolean isCritical();

    GanttCalendar getThird();

    void applyThirdDateConstraint();

    int getThirdDateConstraint();

    void setThirdDate(GanttCalendar thirdDate);

    void setThirdDateConstraint(int dateConstraint);

    TaskInfo getTaskInfo();

	boolean isProjectTask ();
	
	boolean isSupertask();

	List<Document> getAttachments();

}
