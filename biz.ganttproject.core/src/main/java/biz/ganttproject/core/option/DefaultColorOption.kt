/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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

import biz.ganttproject.core.option.ColorOption.Util.getColor
import biz.ganttproject.core.option.ColorOption.Util.determineColor
import biz.ganttproject.core.option.GPAbstractOption
import biz.ganttproject.core.option.ColorOption
import java.awt.Color

open class DefaultColorOption : GPAbstractOption<Color?>, ColorOption {
  constructor(id: String?) : super(id) {}
  constructor(id: String?, initialValue: Color?) : super(id, initialValue) {}

  override fun getPersistentValue(): String? {
    return if (value == null) null else getColor(value!!)
  }

  override fun loadPersistentValue(value: String) {
    if (value != null) {
      resetValue(determineColor(value), true)
    }
  }
}
