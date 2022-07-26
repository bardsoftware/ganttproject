/*
GanttProject is an opensource project management tool.
Copyright (C) 2022 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package biz.ganttproject.core.option

/**
 * @author apopov77@gmail.com
 */

class ObservableProperty<T>(val name: String, var initValue: T) {

  private val listeners: MutableList<ChangeValueListener> = mutableListOf()

  var value: T = initValue
    set(newValue) {
      val oldValue = field
      field = newValue
      firePropertyChanged(oldValue, newValue)
    }

  fun addListener(listener: ChangeValueListener) {
    listeners.add(listener)
  }

  private fun firePropertyChanged(oldValue: T, newValue: T) {
    for (listener in listeners) {
      listener.changeValue(ChangeValueEvent(name, oldValue, newValue, this))
    }
  }
}
