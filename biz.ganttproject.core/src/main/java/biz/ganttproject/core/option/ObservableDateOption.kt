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

import kotlin.Pair
import org.w3c.util.DateParser
import org.w3c.util.InvalidDateException
import java.util.Date

/**
 * Implementation of [DateOption] that delegates to [ObservableDate].
 */
class ObservableDateOption(
  private val delegate: ObservableDate
) : GPAbstractOption<Date>(delegate.id, delegate.value?.let { DateParser.toJavaDate(it) }), DateOption {

  constructor(id: String, value: Date? = null) : this(ObservableDate(id, DateParser.toLocalDate(value)))

  private var myValueValidator: ((Date) -> Pair<Boolean, String>)? = null

  init {
    delegate.addWatcher { event ->
      resetValue(event.newValue?.let { DateParser.toJavaDate(it) }, false, event.trigger)
    }
  }

  override fun getValue(): Date? = delegate.value?.let { DateParser.toJavaDate(it) }

  override fun setValue(value: Date?) {
    setValue(value, null)
  }

  override fun setValue(value: Date?, clientId: Any?) {
    delegate.set(value?.let { DateParser.toLocalDate(it) }, clientId)
  }

  override fun getPersistentValue(): String? =
    getValue()?.let { DateParser.getIsoDateNoHours(it) }

  override fun loadPersistentValue(value: String?) {
    try {
      setValue(value?.let { DateParser.parse(it) })
    } catch (e: InvalidDateException) {
      e.printStackTrace()
    }
  }

  override fun getValueValidator(): ((Date) -> Pair<Boolean, String>)? {
    return myValueValidator
  }

  override fun setValueValidator(validator: ((Date) -> Pair<Boolean, String>)?) {
    myValueValidator = validator
  }

  override fun getIsWritableProperty(): ObservableProperty<Boolean> = delegate.isWritable as ObservableProperty<Boolean>

  override fun isWritable(): Boolean = delegate.isWritable.value

  override fun visitPropertyPaneBuilder(builder: PropertyPaneBuilder) {
    builder.date(delegate)
  }
}
