/*
Copyright 2022 BarD Software s.r.o, Edgar Zhavoronkov

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
package cloud.ganttproject.colloboque;

import biz.ganttproject.core.calendar.AlwaysWorkingTimeCalendarImpl
import biz.ganttproject.core.calendar.GPCalendarCalc
import biz.ganttproject.core.option.BooleanOption
import biz.ganttproject.core.option.ColorOption
import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.DefaultColorOption
import biz.ganttproject.core.time.TimeUnitStack
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import net.sourceforge.ganttproject.gui.NotificationManager
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.RoleManager
import net.sourceforge.ganttproject.roles.RoleManagerImpl
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.CustomColumnsManager
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskManagerConfig
import java.awt.Color
import java.net.URL


object TestSetupHelper {
  fun newTaskManagerBuilder(): TaskManagerBuilder {
    return TaskManagerBuilder()
  }

  class TaskManagerBuilder : TaskManagerConfig {
    private var myGPCalendar: GPCalendarCalc = AlwaysWorkingTimeCalendarImpl()
    private val myTimeUnitStack: TimeUnitStack
    private val myResourceManager: HumanResourceManager
    private val myRoleManager: RoleManager
    private val myDefaultColorOption = DefaultColorOption("taskcolor", Color.CYAN)
    private val mySchedulerDisabledOption = DefaultBooleanOption("scheduler.disabled", false)
    private var taskUpdateBuilderFactory: ProjectDatabase.TaskUpdateBuilder.Factory? = null

    init {
      myTimeUnitStack = GPTimeUnitStack()
      myRoleManager = RoleManagerImpl()
      myResourceManager = HumanResourceManager(myRoleManager.defaultRole, CustomColumnsManager(), myRoleManager)
    }

    override fun getDefaultColor(): Color {
      return myDefaultColorOption.value!!
    }

    override fun getDefaultColorOption(): ColorOption {
      return myDefaultColorOption
    }

    override fun getCalendar(): GPCalendarCalc {
      return myGPCalendar
    }

    override fun getTimeUnitStack(): TimeUnitStack {
      return myTimeUnitStack
    }

    override fun getResourceManager(): HumanResourceManager {
      return myResourceManager
    }

    override fun getProjectDocumentURL(): URL? {
      return null
    }

    fun build(): TaskManager {
      return TaskManager.Access.newInstance(null, this, taskUpdateBuilderFactory)
    }

    override fun getNotificationManager(): NotificationManager? {
      return null
    }

    override fun getSchedulerDisabledOption(): BooleanOption {
      return mySchedulerDisabledOption
    }

    fun setTaskUpdateBuilderFactory(factory: ProjectDatabase.TaskUpdateBuilder.Factory?) {
      taskUpdateBuilderFactory = factory
    }
  }
}
