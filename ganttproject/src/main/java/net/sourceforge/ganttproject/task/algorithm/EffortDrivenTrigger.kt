/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

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
package net.sourceforge.ganttproject.task.algorithm

import net.sourceforge.ganttproject.resource.ResourceEvent
import net.sourceforge.ganttproject.resource.ResourceView
import net.sourceforge.ganttproject.task.TaskManager

/**
 * Derives durations of effort driven tasks when a change of resources can alter the hours available to them:
 * assignments added, removed or given another load, hours per day edited, a resource deleted.
 * Changes of the task itself are handled when its mutator is committed.
 */
class EffortDrivenTrigger(private val taskManager: TaskManager) : ResourceView {
  override fun resourceAdded(event: ResourceEvent?) {}
  override fun resourcesRemoved(event: ResourceEvent?) = recalculate()
  override fun resourceChanged(e: ResourceEvent?) = recalculate()
  override fun resourceAssignmentsChanged(e: ResourceEvent?) = recalculate()
  override fun resourceStructureChanged() {}
  override fun resourceModelReset() {}

  private fun recalculate() {
    taskManager.algorithmCollection.effortDrivenDurationAlgorithm.run()
  }
}
