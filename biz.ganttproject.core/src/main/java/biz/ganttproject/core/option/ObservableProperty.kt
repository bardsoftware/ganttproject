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

interface GPObservable<T> {
  val value: T
  fun addWatcher(watcher: ObservableWatcher<T>)
}

typealias ObservableWatcher<T> = (ObservableEvent<T>) -> Unit
data class ObservableEvent<T>(val property: String, val oldValue: T, val newValue: T, val trigger: Any?)
/**
 * @author apopov77@gmail.com
 */

class ObservableProperty<T>(val name: String, val initValue: T): GPObservable<T> {

  private val listeners: MutableList<ChangeValueListener> = mutableListOf()
  private val watchers = mutableListOf<ObservableWatcher<T>>()

  override val value: T get() = mutableValue

  override fun addWatcher(watcher: ObservableWatcher<T>) {
    watchers.add(watcher)
  }

  var mutableValue: T = initValue
  fun set(newValue: T, trigger: Any? = null) {
    val oldValue = mutableValue
    mutableValue = newValue
    firePropertyChanged(oldValue, newValue, trigger)
  }

  fun addListener(listener: ChangeValueListener) {
    listeners.add(listener)
  }

  private fun firePropertyChanged(oldValue: T, newValue: T, trigger: Any?) {
    for (listener in listeners) {
      listener.changeValue(ChangeValueEvent(name, oldValue, newValue, this))
    }
    val evt = ObservableEvent(name, oldValue, newValue, trigger)
    watchers.forEach { it(evt) }
  }
}
