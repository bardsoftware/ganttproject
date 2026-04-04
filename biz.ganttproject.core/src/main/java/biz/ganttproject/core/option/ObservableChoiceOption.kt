/*
Copyright (C) 2003-2026 Dmitry Barashev, BarD Software s.r.o.

GanttProject is an opensource project management tool.

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

import javafx.util.StringConverter
import java.util.function.Function

/**
 * Implementation of [EnumerationOption] that delegates to [ObservableChoice].
 */
open class ObservableChoiceOption<T>(
  private val delegate: ObservableChoice<T>
) : GPAbstractOption<String>(delegate.id, delegate.converter.toString(delegate.value)), EnumerationOption {

  constructor(id: String, value: T, allValues: List<T>, converter: StringConverter<T>) : this(ObservableChoice(id, value, allValues, converter))
  private var myValueLocalizer: Function<String, String>? = null
  var displayOptions: DropdownDisplayOptions<T>.() -> Unit = {}

  init {
    delegate.addWatcher { event ->
      resetValue(delegate.converter.toString(event.newValue), false, event.trigger)
    }
  }

  override fun getValue(): String = delegate.converter.toString(delegate.value)

  override fun setValue(value: String?) {
    setValue(value, null)
  }

  override fun setValue(value: String?, clientId: Any?) {
    if (value == null) return
    val choiceValue = delegate.allValues.find { delegate.converter.toString(it) == value }
    if (choiceValue != null) {
      delegate.set(choiceValue, clientId)
    }
  }

  override fun getAvailableValues(): Array<String> =
    delegate.allValues.map { delegate.converter.toString(it) }.toTypedArray()

  override fun setValueLocalizer(localizer: Function<String, String>?) {
    myValueLocalizer = localizer
  }

  override fun getValueLocalizer(): Function<String, String>? = myValueLocalizer

  override fun getPersistentValue(): String = value

  override fun loadPersistentValue(value: String?) {
    setValue(value)
  }

  fun getSelectedValue(): T = delegate.value

  fun setSelectedValue(value: T) {
    delegate.value = value
  }

  override fun visitPropertyPaneBuilder(builder: PropertyPaneBuilder) {
    builder.dropdown(this.delegate, this.displayOptions)
  }
}
