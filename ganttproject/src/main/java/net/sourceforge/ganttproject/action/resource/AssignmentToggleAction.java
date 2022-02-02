/*
Copyright 2019 BarD Software s.r.o, Juanan Pereira

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
package net.sourceforge.ganttproject.action.resource;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.ResourceAssignmentMutator;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;


/**
 * Action that adds/removes the assignment of a task to a resource
 */
public class AssignmentToggleAction extends GPAction {
  private final HumanResource myHumanResource;
  private final Task myTask;
  private final GPUndoManager myUndoManager;

  public AssignmentToggleAction(HumanResource hr, Task task, GPUndoManager undoManager) {
    super(hr.getName(), IconSize.TOOLBAR_SMALL);
    myHumanResource = hr;
    myTask = task;
    myUndoManager = undoManager;
  }

  private void createAssignment() {
    ResourceAssignmentMutator mutator = myTask.getAssignmentCollection().createMutator();
    ResourceAssignment newAssignment = mutator.addAssignment(myHumanResource);
    newAssignment.setLoad(100);
    newAssignment.setRoleForAssignment(newAssignment.getResource().getRole());
    mutator.commit();
  }

  public void delete(HumanResource hr) {
    Arrays.stream(myTask.getAssignments()).filter(asg -> asg.getResource() == hr).forEach(asg -> asg.delete());
    ResourceAssignmentMutator mutator = myTask.getAssignmentCollection().createMutator();
    mutator.deleteAssignment(hr);
    mutator.commit();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    myUndoManager.undoableEdit(getLocalizedDescription(), new Runnable() {
      @Override
      public void run() {
        if (getValue(Action.SELECTED_KEY) == Boolean.TRUE) {
          createAssignment();
        } else {
          delete(myHumanResource);
        }
      }
    });
  }
}
