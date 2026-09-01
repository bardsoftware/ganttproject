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

import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.customproperty.CustomColumnsManager
import net.sourceforge.ganttproject.roles.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * [HumanResource.getDaysOff] hands out an unmodifiable view of the resource's days off. These two
 * tests pin both halves of that sentence.
 *
 * Unmodifiable is what makes the notification correct: every change now goes through addDaysOff,
 * removeDaysOff or clearDaysOff, and each of them resets the load distribution and fires. A caller
 * able to mutate the handed-out list could change the resource behind the back of both.
 *
 * A view rather than a copy is what keeps the list cheap and current: a caller holding on to it
 * keeps seeing the resource, and no caller in the tree is handed a snapshot that silently goes
 * stale.
 */
class HumanResourceDaysOffViewTest {
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

  private fun september(day: Int): Date = CalendarFactory.createGanttCalendar(2026, 8, day).time

  private fun daysOff(from: Int, to: Int) = GanttDaysOff(september(from), september(to))

  private fun newPerson(): HumanResource {
    val manager = HumanResourceManager(null as Role?, CustomColumnsManager())
    val person = manager.newHumanResource()
    person.name = "Tester"
    manager.add(person)
    return person
  }

  @Test
  fun `the list handed out by getDaysOff cannot be modified`() {
    val person = newPerson()
    person.addDaysOff(daysOff(9, 10))
    val handedOut = person.daysOff

    assertThrows<UnsupportedOperationException>("clearing the handed-out list must not be possible") {
      handedOut.clear()
    }
    assertThrows<UnsupportedOperationException>("adding to the handed-out list must not be possible") {
      handedOut.add(0, daysOff(20, 21))
    }
    assertThrows<UnsupportedOperationException>("removing from the handed-out list must not be possible") {
      handedOut.removeAt(0)
    }
    assertThrows<UnsupportedOperationException>("overwriting in the handed-out list must not be possible") {
      handedOut.set(0, daysOff(20, 21))
    }

    assertEquals(1, person.daysOff.size, "none of the rejected calls may have changed the resource")
  }

  @Test
  fun `the list handed out by getDaysOff is a view and not a copy`() {
    val person = newPerson()
    person.addDaysOff(daysOff(9, 10))

    val handedOut = person.daysOff
    assertEquals(1, handedOut.size, "the day off given before the call must be in the handed-out list")

    person.addDaysOff(daysOff(20, 21))
    assertEquals(
      2, handedOut.size,
      "a day off given AFTER the call must show up too -- a copy would still show one"
    )
    assertEquals(daysOff(20, 21).start, handedOut.get(1).start, "and it must be the new interval")
  }
}
