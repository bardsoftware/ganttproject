package biz.ganttproject.lib.fx

import javafx.scene.control.ListCell

open class GPListCell<T>: ListCell<T>() {
  protected fun whenNotEmpty(item: T?, empty: Boolean, code: GPListCell<T>.(item: T) -> Unit) {
    if (item == null) {
      text = ""
      graphic = null
      return
    }
    super.updateItem(item, empty)
    if (empty) {
      text = ""
      graphic = null
      return
    }
    this.code(item)
  }
}
