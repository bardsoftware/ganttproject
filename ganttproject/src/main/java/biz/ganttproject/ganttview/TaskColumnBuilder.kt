/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package biz.ganttproject.ganttview

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnBuilder
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.table.TableModel
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.lib.fx.TextCellFactory
import biz.ganttproject.lib.fx.createIconColumn
import biz.ganttproject.lib.fx.createIntegerColumn
import de.jensd.fx.glyphs.GlyphIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.event.EventHandler
import javafx.scene.control.TreeTableColumn
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.util.Comparator

/**
 * Column builder customization for the task table. It creates and configures the look and behavior of
 * task-specific columns.
 */
class TaskColumnBuilder(private val taskTableModel: TableModel<Task, TaskDefaultColumn>, customPropertyManager: CustomPropertyManager,
                        undoManager: GPUndoManager,
                        private val nameCellFactory: TextCellFactory<Task, Task>,
                        private val onNameEditCompleted: (Task)->Unit
): ColumnBuilder<Task, TaskDefaultColumn>(taskTableModel, customPropertyManager, undoManager, TaskDefaultColumn::find) {

  var treeColumn: TreeTableColumn<Task, Task>? = null

  override fun createDefaultColumn(modelColumn: TaskDefaultColumn): TreeTableColumn<Task, out Any> {
    return when (modelColumn) {
      TaskDefaultColumn.DURATION -> {
        createIntegerColumn<Task>(modelColumn.name,
          getValue = {
            (taskTableModel.getValueAt(it, modelColumn) as TimeDuration).length
          },
          setValue = { task, value ->
            undoManager.undoableEdit("Edit properties of task ${task.name}") {
              taskTableModel.setValue(value, task, modelColumn)
            }
          }
        )
      }
      TaskDefaultColumn.NAME -> {
          TreeTableColumn<Task, Task>(modelColumn.name).apply {
            setCellValueFactory {
              ReadOnlyObjectWrapper(it.value.value)
            }
            cellFactory = nameCellFactory
            onEditCommit = EventHandler { event ->
              val targetTask: Task = event.rowValue.value
              event.newValue?.let { copyTask ->
                undoManager.undoableEdit("Edit properties of task ${copyTask.name}") {
                  taskTableModel.setValue(copyTask.name, targetTask, modelColumn)
                }
              }
              runBlocking {
                LOGGER.debug("onEditCommit (name): task=$targetTask")
                onNameEditCompleted(targetTask)
              }
            }
            onEditCancel = EventHandler { event ->
              LOGGER.debug("onEditCancel: event=$event")
              LOGGER.error("why cancel?", exception = Exception())
              val targetTask: Task = event.rowValue.value
              runBlocking {
                LOGGER.debug("onEditCancel (name): task=$targetTask")
                onNameEditCompleted(targetTask)
              }
            }
            treeColumn = this
          }
      }
      else -> super.createDefaultColumn(modelColumn)
    }
  }

  override fun postCreateDefaultColumn(
    tableColumn: TreeTableColumn<Task, Any>,
    modelColumn: TaskDefaultColumn,
    viewData: ColumnList.Column
  ) {
    when (modelColumn) {
      TaskDefaultColumn.OUTLINE_NUMBER -> {
        tableColumn.comparator = TaskDefaultColumn.Functions.OUTLINE_NUMBER_COMPARATOR as Comparator<in Any>?
      }
      TaskDefaultColumn.PRIORITY -> {
        createIconColumn<Task, Task.Priority>(
          modelColumn.name,
          getValue = {
            taskTableModel.getValueAt(it, modelColumn) as Task.Priority
          },
          iconFactory = { priority: Task.Priority -> priority.getIcon() },
          RootLocalizer.createWithRootKey("priority")
        )
      }

      else -> {}
    }
    super.postCreateDefaultColumn(tableColumn, modelColumn, viewData)
  }

  override fun onEditCompleted(node: Task) {
    onNameEditCompleted(node)
  }
}

private fun Task.Priority.getIcon(): GlyphIcon<*>? = when (this) {
  Task.Priority.HIGHEST -> FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_UP)
  Task.Priority.HIGH -> FontAwesomeIconView(FontAwesomeIcon.ANGLE_UP)
  Task.Priority.NORMAL -> null
  Task.Priority.LOW -> FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOWN)
  Task.Priority.LOWEST -> FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_DOWN)
}

private val LOGGER = GPLogger.create("TaskTable")