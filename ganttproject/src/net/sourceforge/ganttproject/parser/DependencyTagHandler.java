/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.parser;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;

import org.xml.sax.Attributes;

public class DependencyTagHandler extends AbstractTagHandler implements ParsingListener {
  private final TaskManager myTaskManager;

  private final UIFacade myUIFacade;

  public DependencyTagHandler(ParsingContext context, TaskManager taskManager, UIFacade uiFacade) {
    super("depend");
    myContext = context;
    myTaskManager = taskManager;
    myUIFacade = uiFacade;
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    loadDependency(attrs);
    return true;
  }

  @Override
  public void parsingStarted() {
  }

  @Override
  public void parsingFinished() {
    for (int i = 0; i < getDependencies().size(); i++) {
      GanttDependStructure ds = getDependencies().get(i);
      Task dependee = myTaskManager.getTask(ds.taskID);
      Task dependant = myTaskManager.getTask(ds.successorTaskID);
      if (dependee == null || dependant == null) {
        continue;
      }

      try {
        TaskDependency dep = myTaskManager.getDependencyCollection().createDependency(dependant, dependee,
            new FinishStartConstraintImpl());
        dep.setConstraint(myTaskManager.createConstraint(ds.dependType));
        dep.setDifference(ds.difference);
        if (myContext.getTasksWithLegacyFixedStart().contains(dependant)) {
          dep.setHardness(TaskDependency.Hardness.RUBBER);
        } else {
          dep.setHardness(ds.myHardness);
        }
      } catch (TaskDependencyException e) {
        GPLogger.log(e);
      }
    }
  }

  protected void loadDependency(Attributes attrs) {
    if (attrs != null) {
      GanttDependStructure gds = new GanttDependStructure();
      gds.setTaskID(getDependencyAddressee());
      gds.setDependTaskID(getDependencyAddresser(attrs));
      String dependencyTypeAsString = attrs.getValue("type");
      String differenceAsString = attrs.getValue("difference");
      String hardnessAsString = attrs.getValue("hardness");
      if (dependencyTypeAsString != null) {
        try {
          gds.setDependType(TaskDependencyConstraint.Type.fromPersistentValue(dependencyTypeAsString));
        } catch (NumberFormatException e) {
        }
      }
      if (differenceAsString != null) {
        try {
          int difference = Integer.parseInt(differenceAsString);
          gds.setDifference(difference);
        } catch (NumberFormatException e) {
        }
      }
      if (hardnessAsString != null) {
        TaskDependency.Hardness hardness = TaskDependency.Hardness.parse(hardnessAsString);
        gds.setHardness(hardness);
      }
      getDependencies().add(gds);
    }
  }

  private int getDependencyAddressee() {
    return getContext().peekTask().getTaskID();
  }

  private int getDependencyAddresser(Attributes attrs) {
    try {
      return Integer.parseInt(attrs.getValue("id"));
    } catch (NumberFormatException e) {
      throw new RuntimeException("Failed to parse 'depend' tag. Attribute 'id' seems to be invalid: "
          + attrs.getValue("id"), e);
    }
  }

  private List<GanttDependStructure> getDependencies() {
    return myDependencies;
  }

  private ParsingContext getContext() {
    return myContext;
  }

  private List<GanttDependStructure> myDependencies = new ArrayList<GanttDependStructure>();

  private ParsingContext myContext;

  private class GanttDependStructure {
    public int taskID, successorTaskID;

    public int difference = 0;

    public TaskDependencyConstraint.Type dependType = TaskDependencyConstraint.Type.finishstart;

    private Hardness myHardness = TaskDependency.Hardness.STRONG;

    public void setHardness(Hardness hardness) {
      myHardness = hardness;
    }

    public GanttDependStructure() {
    }

    public void setTaskID(int taskID) {
      this.taskID = taskID;
    }

    public void setDifference(int difference) {
      this.difference = difference;
    }

    public void setDependTaskID(int successorTaskID) {
      this.successorTaskID = successorTaskID;
    }

    public void setDependType(TaskDependencyConstraint.Type dependType) {
      this.dependType = dependType;
    }
  }
}
