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
package net.sourceforge.ganttproject.task.algorithm;

import java.util.ArrayList;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class FindPossibleDependeesAlgorithmImpl implements FindPossibleDependeesAlgorithm {
  private TaskContainmentHierarchyFacade myContainmentFacade;

  public FindPossibleDependeesAlgorithmImpl() {
  }

  @Override
  public Task[] run(Task dependant) {
    myContainmentFacade = createContainmentFacade();
    ArrayList<Task> result = new ArrayList<Task>();
    Task root = myContainmentFacade.getRootTask();
    Task[] nestedTasks = myContainmentFacade.getNestedTasks(root);
    processTask(nestedTasks, dependant, result);
    return result.toArray(new Task[0]);
  }

  protected abstract TaskContainmentHierarchyFacade createContainmentFacade();

  private void processTask(Task[] taskList, Task dependant, ArrayList<Task> result) {
    for (int i = 0; i < taskList.length; i++) {
      Task next = taskList[i];
      if (!next.equals(dependant)) {
        Task[] nested = myContainmentFacade.getNestedTasks(next);
        // if (nested.length==0) {
        result.add(next);
        // }
        // else {
        processTask(nested, dependant, result);
        // }
      }
    }
  }
}
