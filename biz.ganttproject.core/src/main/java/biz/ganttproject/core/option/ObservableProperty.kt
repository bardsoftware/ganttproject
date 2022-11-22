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

data class Completion(val posStart: Int, val posEnd: Int, val text: String)

interface GPObservable<T> {
  val value: T
  fun addWatcher(watcher: ObservableWatcher<T>)
}

typealias ObservableWatcher<T> = (ObservableEvent<T>) -> Unit
data class ObservableEvent<T>(val oldValue: T, val newValue: T, val trigger: Any?)

class ObservableImpl<T>(initValue: T): GPObservable<T> {
  private val watchers = mutableListOf<ObservableWatcher<T>>()
  var mutableValue: T = initValue
  fun set(newValue: T, trigger: Any? = null) {
    val oldValue = mutableValue
    mutableValue = newValue
    firePropertyChanged(oldValue, newValue, trigger)
  }

  override val value: T get() = mutableValue

  override fun addWatcher(watcher: ObservableWatcher<T>) {
    watchers.add(watcher)
  }

  private fun firePropertyChanged(oldValue: T, newValue: T, trigger: Any?) {
    val evt = ObservableEvent(oldValue, newValue, trigger)
    watchers.forEach { it(evt) }
  }

}
sealed class ObservableProperty<T>(val id: String, initValue: T, private val delegate: ObservableImpl<T> = ObservableImpl(initValue))
  : GPObservable<T> by delegate {
  private val _isWritable = ObservableImpl( true)
  val isWritable: GPObservable<Boolean> get() = _isWritable

  fun set(newValue: T, trigger: Any? = null) {
    this.delegate.set(newValue, trigger)
  }
  fun setWritable(value: Boolean) {
    _isWritable.set(value)
  }


}


class ObservableString(
  id: String, initValue: String? = null,
  val validator: ValueValidator<String> = voidValidator,
  val isScreened: Boolean = false)
  : ObservableProperty<String?>(id, initValue) {

    var completions: (String, Int) -> List<Completion> = { _, _ -> emptyList() }
}
class ObservableBoolean(id: String, initValue: Boolean = false): ObservableProperty<Boolean>(id,initValue)
class ObservableEnum<E : Enum<E>>(id: String, initValue: E, val allValues: Array<E>): ObservableProperty<E>(id,initValue)

class ObservableObject<T>(id: String = "", initValue: T?): ObservableProperty<T?>(id, initValue)