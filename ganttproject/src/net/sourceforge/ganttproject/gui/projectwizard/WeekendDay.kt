/*
Copyright 2020 BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui.projectwizard

import biz.ganttproject.core.calendar.GPCalendar
import com.google.common.collect.Lists
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.ButtonModel
import javax.swing.JCheckBox

/**
 * Creates a list of checkboxes which configure weekends in the given calendar instance.
 * The checkboxes will not allow for checking all days as weekend days; once 6 days are
 * checked, the unchecked one becomes disabled.
 */
fun createWeekendCheckBoxes(calendar: GPCalendar, names: Array<String>) : List<JCheckBox> {
  val result: MutableList<JCheckBox> = Lists.newArrayListWithExpectedSize(7)
  var day = Calendar.MONDAY
  val model = Model(calendar, names)
  for (i in 0..6) {
    result.add(JCheckBox().also {
      it.action = model.createCheckBoxAction(day, it.model)
    })
    if (++day > 7) {
      day = 1
    }
  }
  return result

}

private class Model(private val calendar: GPCalendar, private val names: Array<String>) {
  private val day2action = mutableMapOf<Int, CheckBoxAction>()
  fun createCheckBoxAction(day: Int, buttonModel: ButtonModel): CheckBoxAction {
    return CheckBoxAction(calendar, day, names[day-1], buttonModel, this).also {
      day2action[day] = it
    }
  }

  fun update() {
    var totalSelected = 0
    day2action.values.forEach {
      if (it.myModelButton.isSelected) {
        totalSelected++
      }
    }
    if (totalSelected == 6) {
      day2action.values.firstOrNull { !it.myModelButton.isSelected }?.isEnabled = false
    } else {
      day2action.values.forEach { it.isEnabled = true }
    }
  }
}

private class CheckBoxAction(
    private val myCalendar: GPCalendar,
    private val myDay: Int,
    dayName: String?,
    val myModelButton: ButtonModel,
    private val model: Model) : AbstractAction(dayName) {
  init {
    myModelButton.isSelected = myCalendar.getWeekDayType(myDay) == GPCalendar.DayType.WEEKEND
  }
  override fun actionPerformed(e: ActionEvent) {
    myCalendar.setWeekDayType(
        myDay, if (myModelButton.isSelected) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING
    )
    model.update()
  }
}
