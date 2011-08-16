/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

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
package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

/**
 * @author bard
 */
public class TaskDependencyImpl implements TaskDependency {
    private TaskDependencyConstraint myConstraint;

    private int myDifference;

    private final Task myDependant;

    private final Task myDependee;

    private Hardness myHardness;

    private TaskDependencyCollectionImpl myCollection;

    public TaskDependencyImpl(Task dependant, Task dependee,
            TaskDependencyCollectionImpl collection) {
        myDependant = dependant;
        myDependee = dependee;
        myCollection = collection;
        if (dependee == null || dependant == null) {
            throw new IllegalArgumentException(
                    "invalid participants of dependency: dependee=" + dependee
                            + " dependant=" + dependant);
        }
        myHardness = Hardness.STRONG;
    }

    public Task getDependant() {
        return myDependant;
    }

    public Task getDependee() {
        return myDependee;
    }

    public void setConstraint(TaskDependencyConstraint constraint) {
        myConstraint = constraint;
        constraint.setTaskDependency(this);
        myCollection.fireChanged(this);
    }

    public TaskDependencyConstraint getConstraint() {
        return myConstraint;
    }

    public ActivityBinding getActivityBinding() {
        return getConstraint().getActivityBinding();
    }

    public void delete() {
        myCollection.delete(this);
    }

    public boolean equals(Object obj) {
        boolean result = obj instanceof TaskDependency;
        if (result) {
            TaskDependency rvalue = (TaskDependency) obj;
            result = myDependant.equals(rvalue.getDependant())
                    && myDependee.equals(rvalue.getDependee());
        }
        return result;
    }

    public int hashCode() {
        return 7 * myDependant.hashCode() + 9 * myDependee.hashCode();
    }

    public void setDifference(int difference) {
        myDifference = difference;
    }

    public int getDifference() {
        return myDifference;
    }

    public Hardness getHardness() {
        return myHardness;
    }

    public void setHardness(Hardness hardness) {
        myHardness = hardness;
    }

    public String toString() {
        return myDependant+"->"+myDependee;
    }
}
