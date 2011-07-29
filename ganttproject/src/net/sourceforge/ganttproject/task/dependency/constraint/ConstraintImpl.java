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

import java.util.Date;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;

/**
 * @author bard
 */
public abstract class ConstraintImpl implements Cloneable{
    private final int myID;

    private final String myName;

    private TaskDependency myDependency;

    public ConstraintImpl(int myID, String myName) {
        this.myID = myID;
        this.myName = myName;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    protected TaskDependency getDependency() {
        return myDependency;
    }

    public void setTaskDependency(TaskDependency dependency) {
        myDependency = dependency;
    }

    public String getName() {
        return myName;
    }

    public int getID() {
        return myID;
    }

    public String toString() {
        return getName();
    }

    protected void shift(GanttCalendar calendar, int shift) {
        if (shift != 0) {
            Date shifted = shift(calendar.getTime(), shift);
            calendar.setTime(shifted);
        }
    }

    protected Date shift(Date date, int shift) {
        if (shift == 0) {
            // No shifting is required
            return date;
        }
        return myDependency.getDependant().getManager().getCalendar().shiftDate(
                    date, 
                    myDependency.getDependant().getManager().createLength(shift));
    }

    protected void addDelay(GanttCalendar calendar) {
        shift(calendar, myDependency.getDifference());
//        calendar.add(difference);f
//        GanttCalendar solutionStart = calendar.Clone();
//        solutionStart.add(-1 * myDependency.getDifference());
//        for (int i = 0; i <= difference; i++) {
//            if ((myDependency.getDependant()
//                    .getManager().getCalendar()).isNonWorkingDay(solutionStart
//                    .getTime())) {
//                calendar.add(1);
//                difference++;
//            }
//            solutionStart.add(1);
//        }
    }

    public TaskDependencyConstraint.Collision getBackwardCollision(Date dependantStart) {
        return null;
    }

    public abstract TaskDependencyConstraint.Collision getCollision();
}
