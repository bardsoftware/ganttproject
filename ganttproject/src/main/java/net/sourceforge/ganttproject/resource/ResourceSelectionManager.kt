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
package net.sourceforge.ganttproject.resource

import net.sourceforge.ganttproject.task.ResourceAssignment

typealias ResourceSelectionListener = (selection: List<HumanResource>, trigger: Any) -> Unit
typealias AssignmentSelectionListener = (selection: List<ResourceAssignment>, trigger: Any) -> Unit

/**
 * Manages resource and assignment selection in the application window.
 */
class ResourceSelectionManager: ResourceContext, AssignmentContext {
  private val selection: MutableList<HumanResource> = mutableListOf()
  private val listeners: MutableList<ResourceSelectionListener> = mutableListOf()

  private val assignmentSelection: MutableList<ResourceAssignment> = mutableListOf()
  private val assignmentListeners: MutableList<AssignmentSelectionListener> = mutableListOf()
  override fun getResources(): List<HumanResource> = selection.toList()
  override fun getResourceAssignments(): List<ResourceAssignment> = assignmentSelection.toList()

  fun select(resources: List<HumanResource>, replace: Boolean = true, trigger: Any) {
    selection.toMutableSet().also {
      if (replace) {
        it.clear()
      }
      it.addAll(resources)
      val fireEvent = (selection != it)
      selection.clear()
      selection.addAll(it)
      if (fireEvent) {
        fireSelectionChanged(selection, trigger)
      }
    }
  }

  fun select(assignments: List<ResourceAssignment>, trigger: Any) {
    assignmentSelection.toMutableSet().also {
      assignmentSelection.clear()
      assignmentSelection.addAll(assignments)
      if (it != assignmentSelection.toSet()) {
        fireAssignmentSelectionChanged(assignmentSelection, trigger)
      }
    }

  }

  private fun fireAssignmentSelectionChanged(assignmentSelection: List<ResourceAssignment>, trigger: Any) {
    assignmentListeners.forEach {
      try {
        it(assignmentSelection, trigger)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun addResourceListener(listener: ResourceSelectionListener) = listeners.add(listener)

  fun addAssignmentListener(listener: AssignmentSelectionListener) = assignmentListeners.add(listener)

  private fun fireSelectionChanged(selection: List<HumanResource>, trigger: Any) =
    listeners.forEach {
      try {
        it(selection, trigger)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
}
