/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.task.dependency.constraint;

import java.util.Calendar;
import java.util.Date;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTaskRelationship;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.ActivityBinding;

/**
 * Dependent task finishes not earlier than dependee finishes
 * @author bard
 */
public class FinishFinishConstraintImpl extends ConstraintImpl implements
        TaskDependencyConstraint {
    public FinishFinishConstraintImpl() {
        super(GanttTaskRelationship.FF, GanttLanguage.getInstance().getText(
                "finishfinish"));
    }

    public TaskDependencyConstraint.Collision getCollision() {
        TaskDependencyConstraint.Collision result = null;
        Task dependee = getDependency().getDependee();
        Task dependant = getDependency().getDependant();
        GanttCalendar dependeeEnd = dependee.getEnd();
        GanttCalendar dependantEnd = dependant.getEnd();

        int difference = getDependency().getDifference();

        GanttCalendar comparisonDate = dependeeEnd.clone();
        comparisonDate.add(Calendar.DATE, difference);

        boolean isActive = getDependency().getHardness()==TaskDependency.Hardness.RUBBER ? dependantEnd
                .compareTo(comparisonDate) < 0 : dependantEnd
                .compareTo(comparisonDate) != 0;

        GanttCalendar acceptableStart = dependant.getStart().clone();

        // GanttCalendar acceptableStart = dependant.getStart();
        if (isActive) {
            Task clone = dependee.unpluggedClone();
            TaskMutator mutator = clone.createMutator();
            mutator.shift(-dependant.getDuration().getLength());
            acceptableStart = clone.getEnd();
        }
        addDelay(acceptableStart);
        result = new TaskDependencyConstraint.DefaultCollision(acceptableStart,
                TaskDependencyConstraint.Collision.START_LATER_VARIATION,
                isActive);

        return result;
    }

    public Collision getBackwardCollision(Date dependantStart) {
        Task dependee = getDependency().getDependee();
        GanttCalendar dependeeEnd = dependee.getEnd().clone();

        Date barrier = shift(
                dependantStart,
                getDependency().getDependant().getDuration().getLength() - getDependency().getDifference());
        boolean isActive = getDependency().getHardness() == TaskDependency.Hardness.RUBBER ?
                dependeeEnd.getTime().compareTo(barrier) > 0
                : dependeeEnd.getTime().compareTo(barrier) != 0;

        return new TaskDependencyConstraint.DefaultCollision(
                new GanttCalendar(barrier),
                TaskDependencyConstraint.Collision.START_EARLIER_VARIATION,
                isActive);
    }

    public ActivityBinding getActivityBinding() {
        TaskActivity[] dependantActivities = getDependency().getDependant()
                .getActivities();
        TaskActivity[] dependeeActivities = getDependency().getDependee()
                .getActivities();
        TaskActivity theDependant = dependantActivities[dependantActivities.length - 1];
        TaskActivity theDependee = dependeeActivities[dependeeActivities.length - 1];
        return new DependencyActivityBindingImpl(theDependant, theDependee,
                new Date[] { theDependant.getEnd(), theDependee.getEnd() });
    }
}
