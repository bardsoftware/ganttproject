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
package net.sourceforge.ganttproject.gui

import javafx.scene.control.Tab
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GanttTask
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.OkAction
import net.sourceforge.ganttproject.gui.taskproperties.TaskPropertiesController
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import java.awt.event.ActionEvent
import java.text.MessageFormat

class GanttDialogProperties(private val tasks: Array<GanttTask?>) {
    fun show(project: IGanttProject, uiFacade: UIFacade) {
        val language = GanttLanguage.getInstance()
        val taskPropertiesBean = GanttTaskPropertiesBean(tasks, project, uiFacade)
        val taskPropertiesController = TaskPropertiesController(tasks[0]!!, project.projectDatabase, uiFacade)

        val okAction = OkAction.create("ok") {
            uiFacade.getUndoManager().undoableEdit(language.getText("properties.changed"), Runnable {
                val mutator = taskPropertiesController.save()
                taskPropertiesBean.save(mutator)
                try {
                    project.taskManager.getAlgorithmCollection().recalculateTaskScheduleAlgorithm.run()
                } catch (e: TaskDependencyException) {
                    if (!GPLogger.log(e)) {
                        e.printStackTrace()
                    }
                }
                uiFacade.refresh()
                uiFacade.getActiveChart().focus()
            })
        }

        val cancelAction = CancelAction.create("cancel") {
            taskPropertiesController.cancel()
            uiFacade.getActiveChart().focus()
        }

        val taskNames = StringBuffer()
        for (i in tasks.indices) {
            if (i > 0) {
                taskNames.append(language.getText(if (i + 1 == tasks.size) "list.separator.last" else "list.separator"))
            }
            taskNames.append(tasks[i]!!.name)
        }

        val title = MessageFormat.format(language.getText("properties.task.title"), taskNames)
        val tabProviders = listOf(
            PropertiedDialogTabProvider(
              { tabPane -> tabPane.tabs.add(Tab(
                taskPropertiesController.mainPropertiesPanel.title,
                taskPropertiesController.mainPropertiesPanel.fxComponent
              ))},
              { taskPropertiesController.mainPropertiesPanel.requestFocus() }
            ),
            swingTab(language.getText("predecessors")) { taskPropertiesBean.predecessorsPanel },
            swingTab(language.getText("human")) { taskPropertiesBean.resourcesPanel },
            PropertiedDialogTabProvider(
                { tabPane ->
                    tabPane.tabs.add(
                        Tab(
                            taskPropertiesController.customPropertiesPanel.title,
                            taskPropertiesController.customPropertiesPanel.getFxNode()
                        )
                    )
                },
                { }
            )
        )

        val actions = listOf(okAction, cancelAction)
        propertiesDialog(title, "taskProperties", actions, taskPropertiesController.validationErrors, tabProviders)
    }
}
