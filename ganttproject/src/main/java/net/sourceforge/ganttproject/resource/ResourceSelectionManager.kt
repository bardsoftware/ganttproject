package net.sourceforge.ganttproject.resource

import net.sourceforge.ganttproject.task.ResourceAssignment

typealias ResourceSelectionListener = (selection: List<HumanResource>, trigger: Any) -> Unit
typealias AssignmentSelectionListener = (selection: List<ResourceAssignment>, trigger: Any) -> Unit

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
