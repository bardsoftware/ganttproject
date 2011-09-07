/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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

import java.util.List;

/**
 * @author bard
 */
public interface TaskContainmentHierarchyFacade {
    Task[] getNestedTasks(Task container);
    Task[] getDeepNestedTasks(Task container);

    boolean hasNestedTasks(Task container);

    Task getRootTask();

    Task getContainer(Task nestedTask);

    /** @return the previous sibling or null if task is the first child of the parent task */
    Task getPreviousSibling(Task nestedTask);

    /** @return the next sibling or null if task is the last child of the parent task */
    Task getNextSibling(Task task);

    /** @return the index of the nestedTask with respect of its siblings */
    int getTaskIndex(Task nestedTask);

    /** Move whatMove to whereMove, added as a child at the end */
    void move(Task whatMove, Task whereMove);

    /** Move whatMove to whereMove, added as a child at index */
    void move(Task whatMove, Task whereMove, int index);

    boolean areUnrelated(Task dependant, Task dependee);

    int getDepth(Task task);

    int compareDocumentOrder(Task next, Task dependeeTask);
    List<Task> getTasksInDocumentOrder();

    boolean contains(Task task);

    interface Factory {
        TaskContainmentHierarchyFacade createFacede();
    }

    TaskContainmentHierarchyFacade STUB = new TaskContainmentHierarchyFacade() {
        public Task[] getNestedTasks(Task container) {
            return new Task[0];
        }
        public Task[] getDeepNestedTasks(Task container) {
            // TODO Auto-generated method stub
            return null;
        }
        public boolean hasNestedTasks(Task container) {
            return false;
        }
        public Task getRootTask() {
            return null;
        }
        public Task getContainer(Task nestedTask) {
            return null;
        }
        public Task getPreviousSibling(Task nestedTask) {
            return null;
        }
        public Task getNextSibling(Task nestedTask) {
            return null;
        }
        public int getTaskIndex(Task nestedTask) {
            return 0;
        }
        public void move(Task whatMove, Task whereMove) {
        }
        public void move(Task whatMove, Task whereMove, int index) {
        }
        public boolean areUnrelated(Task dependant, Task dependee) {
            return false;
        }
        public int getDepth(Task task) {
            return 0;
        }
        public int compareDocumentOrder(Task next, Task dependeeTask) {
            throw new UnsupportedOperationException();
        }
        public boolean contains(Task task) {
            throw new UnsupportedOperationException();
        }
        @Override
        public List<Task> getTasksInDocumentOrder() {
            throw new UnsupportedOperationException();
        }
    };
}
