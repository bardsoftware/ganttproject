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

import java.util.function.Function

/**
 * Implementation of [EnumerationOption] that delegates to [ObservableEnum].
 */
class ObservableEnumerationOption<E : Enum<E>>(
  private val delegate: ObservableEnum<E>
) : GPAbstractOption<String>(delegate.id, delegate.value.name), EnumerationOption {

  constructor(id: String, value: E, allValues: List<E>) : this(ObservableEnum(id, value, allValues))
  private var myValueLocalizer: Function<String, String>? = null

  init {
    delegate.addWatcher { event ->
      resetValue(event.newValue.name, false, event.trigger)
    }
  }

  override fun getValue(): String = delegate.value.name

  override fun setValue(value: String?) {
    setValue(value, null)
  }

  override fun setValue(value: String?, clientId: Any?) {
    if (value == null) return
    val enumValue = delegate.allValues.find { it.name == value }
    if (enumValue != null) {
      delegate.set(enumValue, clientId)
    }
  }

  override fun getAvailableValues(): Array<String> =
    delegate.allValues.map { it.name }.toTypedArray()

  override fun setValueLocalizer(localizer: Function<String, String>?) {
    myValueLocalizer = localizer
  }

  override fun getValueLocalizer(): Function<String, String>? = myValueLocalizer

  override fun getPersistentValue(): String = value

  override fun loadPersistentValue(value: String?) {
    setValue(value)
  }

  fun getSelectedValue(): E = delegate.value

  fun setSelectedValue(value: E) {
    delegate.value = value
  }

  override fun visitPropertyPaneBuilder(builder: PropertyPaneBuilder) {
    builder.dropdown(delegate, null)
  }
}
