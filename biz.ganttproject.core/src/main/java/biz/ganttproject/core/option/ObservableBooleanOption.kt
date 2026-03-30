/*
GanttProject is an opensource project management tool.
Copyright (C) 2024 Dmitry Barashev, GanttProject Team

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
 * Implementation of [BooleanOption] that delegates to [ObservableBoolean].
 */
class ObservableBooleanOption(
    private val delegate: ObservableBoolean
) : GPAbstractOption<Boolean>(delegate.id, delegate.value), BooleanOption {

    constructor(id: String, value: Boolean = false) : this(ObservableBoolean(id, value))

    init {
        delegate.addWatcher { event ->
            resetValue(event.newValue, false, event.trigger)
        }
    }

    override fun getValue(): Boolean = delegate.value

    override fun setValue(value: Boolean?) {
        setValue(value, null)
    }

    override fun setValue(value: Boolean?, clientId: Any?) {
        if (value == null) return
        delegate.set(value, clientId)
    }

    override fun isChecked(): Boolean = delegate.value

    override fun toggle() {
        delegate.value = !delegate.value
    }

    override fun getIsWritableProperty(): ObservableProperty<Boolean> = delegate.isWritable as ObservableProperty<Boolean>

    override fun isWritable(): Boolean = delegate.isWritable.value

    override fun getPersistentValue(): String = delegate.value.toString()

    override fun loadPersistentValue(value: String?) {
        setValue(value?.toBoolean())
    }

    override fun visitPropertyPaneBuilder(builder: PropertyPaneBuilder) {
        builder.checkbox(delegate)
    }
}
