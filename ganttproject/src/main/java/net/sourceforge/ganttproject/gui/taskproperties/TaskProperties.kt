/*
Copyright 2025 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.gui.taskproperties

import javafx.collections.FXCollections
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskMutator

class TaskPropertiesController(private val task: Task, private val projectDatabase: ProjectDatabase, private val uiFacade: UIFacade) {

  val mainPropertiesPanel by lazy {
    MainPropertiesPanel(task, uiFacade.getCurrentTaskView()).also {
      it.defaultColor = uiFacade.ganttChart.taskDefaultColorOption
      it.validationErrors.subscribe {
        validationErrors.clear()
        validationErrors.addAll(it.validationErrors)
      }
    }
  }

  val customPropertiesPanel by lazy {
    CustomColumnsPanel(task.manager.customPropertyManager, projectDatabase, CustomColumnsPanel.Type.TASK,
      uiFacade.undoManager, task.customValues.copyOf(), uiFacade.taskColumnList)
  }

  val validationErrors = FXCollections.observableArrayList<String>()

  fun save(): TaskMutator =
    task.createMutator().also { mutator ->
      mainPropertiesPanel.save(mutator)
      customPropertiesPanel.save {
        mutator.setCustomProperties(it)
      }
    }

  fun cancel() {
    customPropertiesPanel.cancel()
  }

}