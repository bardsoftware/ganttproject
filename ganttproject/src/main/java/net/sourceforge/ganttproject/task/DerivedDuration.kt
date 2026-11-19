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
package net.sourceforge.ganttproject.task

/**
 * Tells if the duration (and thus the end) of a task is derived from its effort, so that it must not be edited.
 * Milestones and summary tasks are not effort driven, whatever the switch says.
 */
fun isDurationDerived(isEffortDriven: Boolean, isMilestone: Boolean, isSummary: Boolean): Boolean =
  isEffortDriven && !isMilestone && !isSummary

fun Task.hasDerivedDuration(): Boolean = isDurationDerived(isEffortDriven, isMilestone, isSupertask)
