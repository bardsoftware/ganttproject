/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sourceforge.ganttproject.task

internal fun TaskManager.TaskBuilder.setupNewTask(task: TaskImpl, manager: TaskManagerImpl) {
  val name = myName ?: run {
    myPrototype?.name ?: manager.taskNamePrefixOption.value.toString() + "_" + task.taskID
  }
  task.name = name

  val duration = myDuration ?: myPrototype?.duration ?: run {
    if (myEndDate == null) {
      manager.createLength(manager.timeUnitStack.defaultTimeUnit, 1.0f)
    } else {
      manager.createLength(manager.timeUnitStack.defaultTimeUnit, myStartDate, myEndDate)
    }
  }
  duration?.let { task.duration = it }

  myPrototype?.let {
    task.isMilestone = it.isMilestone
    task.isProjectTask = it.isProjectTask
    task.shape = it.shape
    it.customValues.customProperties.forEach { prop ->
      task.customValues.setValue(prop.definition, prop.value)
    }
    if (it.thirdDateConstraint == 1) {
      task.thirdDateConstraint = it.thirdDateConstraint
      task.setThirdDate(it.third)
    }
  }

  val color = myColor ?: myPrototype?.color
  color?.let { task.color = it }

  val priority = myPriority ?: myPrototype?.priority
  priority?.let { task.priority = it }

  val notes = myNotes ?: myPrototype?.notes
  notes?.let { task.notes = it }

  val webLink = myWebLink ?: myPrototype?.webLink
  webLink?.let { task.webLink = it }

  val completion = myCompletion ?: myPrototype?.completionPercentage
  completion?.let { task.completionPercentage = it }

  myCost?.let {
    task.cost = CostStub(it, false)
  } ?: run {
    myPrototype?.cost?.let {
      task.cost = it
    }
  }
}

fun TaskContainmentHierarchyFacade.depthFirstWalk(root: Task, level: Int = 0, visitor: (Task, Task?, Int, Int) -> Boolean) {
  getNestedTasks(root).let { children ->
    children.forEachIndexed { idx, child ->
      if (visitor(root, child, idx, level)) {
        this.depthFirstWalk(root = child, level = level + 1, visitor = visitor)
      }
    }
    visitor(root, null, children.size, level)
  }
}
