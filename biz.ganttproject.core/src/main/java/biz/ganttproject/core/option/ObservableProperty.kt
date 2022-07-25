package biz.ganttproject.core.option


class ObservableProperty<T>(val name: String, private var initValue: T) {

  private val myListeners: MutableList<ChangeValueListener?> = mutableListOf()

  var value: T = initValue
    set(newValue) {
      val oldValue = field
      field = newValue
      firePropertyChanged(oldValue, newValue)
    }

  fun addListener(listener: ChangeValueListener?) {
    myListeners.add(listener)
  }

  private fun firePropertyChanged(oldValue: T, newValue: T) {
    for (listener in myListeners) {
      listener!!.changeValue(ChangeValueEvent(name, oldValue, newValue, this))
    }
  }
}
