/*
Copyright 2026 BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.resource

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.customproperty.CustomColumnsManager
import net.sourceforge.ganttproject.roles.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Adding and removing days off must be symmetric with respect to notification: whoever listens to
 * a resource has to learn about both, because both change the resource's load distribution.
 *
 * Adding goes through [HumanResource.addDaysOff], which resets the loads and fires. Removing has no
 * such entry point at all -- the only way is to mutate the list handed out by
 * [HumanResource.getDaysOff], and that list is not observed.
 */
class HumanResourceDaysOffTest {
  init {
    // GanttDaysOff builds GanttCalendars, and those need a locale.
    object : CalendarFactory() {
      init {
        setLocaleApi(object : CalendarFactory.LocaleApi {
          override fun getLocale(): Locale = Locale.GERMANY
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMANY)
        })
      }
    }
  }

  private class CountingView : ResourceView {
    var changed = 0
    override fun resourceAdded(event: ResourceEvent) {}
    override fun resourcesRemoved(event: ResourceEvent) {}
    override fun resourceChanged(e: ResourceEvent) { changed++ }
    override fun resourceAssignmentsChanged(e: ResourceEvent) {}
    override fun resourceStructureChanged() {}
    override fun resourceModelReset() {}
  }

  private fun september(day: Int): Date = CalendarFactory.createGanttCalendar(2026, 8, day).time

  @Test
  fun `removing the last day off notifies the listeners just as adding it does`() {
    val manager = HumanResourceManager(null as Role?, CustomColumnsManager())
    val person = manager.newHumanResource()
    person.name = "Tester"
    manager.add(person)

    val view = CountingView()
    manager.addView(view)

    person.addDaysOff(GanttDaysOff(september(9), september(10)))
    assertEquals(1, view.changed, "adding a day off must notify the listeners")

    view.changed = 0
    person.daysOff.clear()
    assertEquals(
      1, view.changed,
      "removing the LAST day off must notify the listeners too -- otherwise nobody recalculates"
    )
  }
}
