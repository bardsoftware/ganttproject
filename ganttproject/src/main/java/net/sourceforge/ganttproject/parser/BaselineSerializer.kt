/*
Copyright 2022 BarD Software s.r.o, GanttProject Cloud OU, Dmitry Barashev

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
package net.sourceforge.ganttproject.parser

import biz.ganttproject.core.io.XmlProject
import biz.ganttproject.core.time.GanttCalendar
import net.sourceforge.ganttproject.GanttPreviousState
import net.sourceforge.ganttproject.GanttPreviousStateTask

class BaselineSerializer() {
  fun loadBaselines(xmlProject: XmlProject, baselines: MutableList<GanttPreviousState>) {
    xmlProject.baselines?.baselines?.forEach { xmlBaseline ->
      val name = xmlBaseline.name
      val tasks = xmlBaseline.tasks?.map { xmlBaselineTask ->
        val id = xmlBaselineTask.id
        val isMilestone = xmlBaselineTask.isMilestone
        val startDate = xmlBaselineTask.startDate
        val duration = xmlBaselineTask.duration
        val isSummaryTask = xmlBaselineTask.isSummaryTask
        GanttPreviousStateTask(id, GanttCalendar.parseXMLDate(startDate), duration, isMilestone, isSummaryTask)
      }?.toList() ?: emptyList()
      GanttPreviousState(name, tasks).also {
        it.init()
        it.saveFile()
        baselines.add(it)
      }
    }
  }
}