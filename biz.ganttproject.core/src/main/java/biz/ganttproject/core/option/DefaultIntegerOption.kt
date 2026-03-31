/*
Copyright 2003-2026 Dmitry Barashev, GanttProject Team, BarD Software s.r.o

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

class DefaultIntegerOption(
    id: String,
    initialValue: Int = 0
) : ObservableAbstractOption<Int>(ObservableInt(id, initialValue)), IntegerOption {

    constructor(id: String) : this(id, 0)

    override fun getPersistentValue(): String = value.toString()

    override fun loadPersistentValue(value: String?) {
        setValue(value?.toInt() ?: 0)
    }

    override fun visitPropertyPaneBuilder(builder: PropertyPaneBuilder) {
        builder.numeric(delegate as ObservableInt)
    }
}
