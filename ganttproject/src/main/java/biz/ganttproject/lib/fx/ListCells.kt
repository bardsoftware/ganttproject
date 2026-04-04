/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o.

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
package biz.ganttproject.lib.fx

import javafx.scene.control.ListCell
import javafx.scene.control.TableCell

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

open class GPTableCell<S, T>: TableCell<S, T>() {
  protected fun whenNotEmpty(item: T?, empty: Boolean, code: GPTableCell<S, T>.(item: T) -> Unit) {
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