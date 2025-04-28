package net.sourceforge.ganttproject.resource

typealias ResourceSelectionListener = (selection: List<HumanResource>, trigger: Any) -> Unit

class ResourceSelectionManager: ResourceContext {
  private val selection: MutableList<HumanResource> = mutableListOf()
  private val listeners: MutableList<ResourceSelectionListener> = mutableListOf()

  override fun getResources(): List<HumanResource> = selection.toList()

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

  fun subscribe(listener: ResourceSelectionListener) = listeners.add(listener)

  private fun fireSelectionChanged(selection: List<HumanResource>, trigger: Any) =
    listeners.forEach {
      try {
        it(selection, trigger)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
}
