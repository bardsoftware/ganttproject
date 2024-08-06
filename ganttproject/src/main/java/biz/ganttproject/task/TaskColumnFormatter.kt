/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package biz.ganttproject.task

import biz.ganttproject.app.Localizer
import biz.ganttproject.app.getNumberFormat
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.ColorOption.Util.getColor
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskProperties

class DefaultTaskColumnFormatter(private val localizer: Localizer) {
  fun formatTaskColumn(task: Task, columnId: String): String? {
    return TaskDefaultColumn.find(columnId)?.let { taskDefaultColumn ->
      when (taskDefaultColumn) {
        TaskDefaultColumn.TYPE -> null
        TaskDefaultColumn.PRIORITY -> localizer.formatText(task.getPriority().i18nKey)
        TaskDefaultColumn.INFO -> null
        TaskDefaultColumn.NAME -> task.name
        TaskDefaultColumn.BEGIN_DATE -> GanttLanguage.getInstance().formatShortDate(task.start)
        TaskDefaultColumn.END_DATE -> GanttLanguage.getInstance().formatShortDate(task.displayEnd)
        TaskDefaultColumn.DURATION -> {
          if (task.isMilestone) { "" }
          else " [ ${task.duration.length} ${localizer.formatText("days")} ] "
        }
        TaskDefaultColumn.COMPLETION -> " [ ${task.completionPercentage}% ] "
        TaskDefaultColumn.COORDINATOR -> TaskProperties.formatCoordinators(task)
        TaskDefaultColumn.PREDECESSORS -> TaskProperties.formatPredecessors(task, ", ", false)
        TaskDefaultColumn.ID -> "# ${task.taskID}"
        TaskDefaultColumn.OUTLINE_NUMBER -> task.manager.taskHierarchy.getOutlinePath(task).joinToString(separator = ".")
        TaskDefaultColumn.COST -> getNumberFormat().format(task.cost.value)
        TaskDefaultColumn.RESOURCES -> TaskProperties.formatResources(task.assignments.toList())
        TaskDefaultColumn.COLOR -> getColor(task.getColor())
        TaskDefaultColumn.NOTES -> null
        TaskDefaultColumn.ATTACHMENTS -> null
      }
    } ?: run {
      task.manager.customPropertyManager.getCustomPropertyDefinition(columnId)?.let {
        def -> task.customValues.getValue(def)?.let { value ->
          if (def.propertyClass.isNumeric) getNumberFormat().format(value) else value.toString()
        }
      }
    }
  }
}
