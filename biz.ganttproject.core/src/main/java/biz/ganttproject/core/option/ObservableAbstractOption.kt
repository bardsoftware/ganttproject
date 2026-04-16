/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.core.option

/**
 * Implementation of [GPOption] that delegates to [ObservableProperty].
 */
abstract class ObservableAbstractOption<T>(
    val delegate: ObservableProperty<T>
) : GPAbstractOption<T>(delegate.id, delegate.value) {

    init {
        delegate.addWatcher { event ->
            resetValue(event.newValue, false, event.trigger)
        }
    }

    override fun getValue(): T = delegate.value

    override fun setValue(value: T) {
        setValue(value, null)
    }

    override fun setValue(value: T, clientId: Any?) {
        delegate.set(value, clientId)
    }

    override fun getIsWritableProperty(): GPObservable<Boolean>? = delegate.isWritable

    override fun isWritable(): Boolean = delegate.isWritable.value
}
